package com.animeblog.service;

import com.animeblog.util.ExpiringCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class LoginAttemptService {

    private final ExpiringCache<LoginAttemptState> loginAttemptCache = new ExpiringCache<>(60);

    @Value("${app.security.turnstile.login.failure-window-millis:1800000}")
    private long failureWindowMillis;

    public int getFailureCount(String username, String clientIp) {
        LoginAttemptState state = loginAttemptCache.get(buildCacheKey(username, clientIp));
        return state == null ? 0 : Math.max(state.failureCount(), 0);
    }

    public int recordFailure(String username, String clientIp) {
        int nextFailureCount = getFailureCount(username, clientIp) + 1;
        loginAttemptCache.put(
                buildCacheKey(username, clientIp),
                new LoginAttemptState(nextFailureCount),
                Math.max(failureWindowMillis, 60_000L)
        );
        return nextFailureCount;
    }

    public void clearFailures(String username, String clientIp) {
        loginAttemptCache.remove(buildCacheKey(username, clientIp));
    }

    public boolean isTurnstileRequired(String username, String clientIp, int failureThreshold) {
        return failureThreshold > 0 && getFailureCount(username, clientIp) >= failureThreshold;
    }

    private String buildCacheKey(String username, String clientIp) {
        String normalizedUsername = StringUtils.hasText(username)
                ? username.trim().toLowerCase(Locale.ROOT)
                : "anonymous";
        String normalizedIp = StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown";
        return normalizedUsername + "|" + normalizedIp;
    }

    private record LoginAttemptState(int failureCount) {
    }
}
