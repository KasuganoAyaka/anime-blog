package com.animeblog.service;

import com.animeblog.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class TurnstileService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileService.class);

    private final ObjectMapper objectMapper;

    @Value("${app.security.turnstile.enabled:false}")
    private boolean enabled;

    @Value("${app.security.turnstile.site-key:}")
    private String siteKey;

    @Value("${app.security.turnstile.secret-key:}")
    private String secretKey;

    @Value("${app.security.turnstile.verify-endpoint:https://challenges.cloudflare.com/turnstile/v0/siteverify}")
    private String verifyEndpoint;

    @Value("${app.security.turnstile.connect-timeout-millis:2500}")
    private long connectTimeoutMillis;

    @Value("${app.security.turnstile.read-timeout-millis:4000}")
    private long readTimeoutMillis;

    @Value("${app.security.turnstile.login.enabled:true}")
    private boolean loginEnabled;

    @Value("${app.security.turnstile.login.failure-threshold:3}")
    private int loginFailureThreshold;

    @Value("${app.security.turnstile.register.email-check-enabled:true}")
    private boolean registerEmailCheckEnabled;

    @Value("${app.security.turnstile.comment.guest-check-enabled:true}")
    private boolean guestCommentCheckEnabled;

    public TurnstileService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return enabled
                && StringUtils.hasText(siteKey)
                && StringUtils.hasText(secretKey)
                && StringUtils.hasText(verifyEndpoint);
    }

    public boolean isLoginProtectionEnabled() {
        return isEnabled() && loginEnabled && loginFailureThreshold > 0;
    }

    public boolean isRegisterEmailProtectionEnabled() {
        return isEnabled() && registerEmailCheckEnabled;
    }

    public boolean isGuestCommentProtectionEnabled() {
        return isEnabled() && guestCommentCheckEnabled;
    }

    public int getLoginFailureThreshold() {
        return Math.max(loginFailureThreshold, 0);
    }

    public Map<String, Object> buildPublicConfig() {
        return Map.of(
                "enabled", isEnabled(),
                "siteKey", isEnabled() ? siteKey : "",
                "login", Map.of(
                        "enabled", isLoginProtectionEnabled(),
                        "failureThreshold", getLoginFailureThreshold()
                ),
                "register", Map.of(
                        "emailCheckEnabled", isRegisterEmailProtectionEnabled()
                ),
                "comment", Map.of(
                        "guestCheckEnabled", isGuestCommentProtectionEnabled()
                )
        );
    }

    public void validateOrThrow(String token, String remoteIp, String expectedAction, String failureMessage) {
        if (!isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(failureMessage);
        }

        TurnstileVerificationResult verificationResult = verifyToken(token, remoteIp);
        if (!verificationResult.success()) {
            throw new BusinessException(failureMessage);
        }
        if (StringUtils.hasText(expectedAction)
                && StringUtils.hasText(verificationResult.action())
                && !expectedAction.equals(verificationResult.action())) {
            log.warn("Turnstile action mismatch. expected={}, actual={}", expectedAction, verificationResult.action());
            throw new BusinessException(failureMessage);
        }
    }

    private TurnstileVerificationResult verifyToken(String token, String remoteIp) {
        try {
            StringBuilder formBody = new StringBuilder();
            appendFormField(formBody, "secret", secretKey);
            appendFormField(formBody, "response", token);
            if (StringUtils.hasText(remoteIp)) {
                appendFormField(formBody, "remoteip", remoteIp.trim());
            }

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMillis, 500L)))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(verifyEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(Math.max(readTimeoutMillis, 500L)))
                    .POST(HttpRequest.BodyPublishers.ofString(formBody.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Turnstile verification returned unexpected status: {}", response.statusCode());
                return new TurnstileVerificationResult(false, "", List.of("http_status_" + response.statusCode()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            boolean success = root.path("success").asBoolean(false);
            String action = root.path("action").asText("");
            List<String> errorCodes = root.path("error-codes").isArray()
                    ? objectMapper.convertValue(root.path("error-codes"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))
                    : List.of();

            if (!success) {
                log.warn("Turnstile verification failed: {}", errorCodes);
            }

            return new TurnstileVerificationResult(success, action, errorCodes);
        } catch (Exception exception) {
            log.warn("Turnstile verification request failed", exception);
            return new TurnstileVerificationResult(false, "", List.of("verification_failed"));
        }
    }

    private void appendFormField(StringBuilder formBody, String key, String value) {
        if (formBody.length() > 0) {
            formBody.append('&');
        }
        formBody
                .append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
    }

    private record TurnstileVerificationResult(boolean success, String action, List<String> errorCodes) {
    }
}
