package com.animeblog.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.animeblog.dto.ApiResponse;
import com.animeblog.dto.LoginRequest;
import com.animeblog.dto.RegisterRequest;
import com.animeblog.entity.EmailCode;
import com.animeblog.entity.User;
import com.animeblog.mapper.EmailCodeMapper;
import com.animeblog.service.LoginAttemptService;
import com.animeblog.service.TurnstileService;
import com.animeblog.mapper.UserMapper;
import com.animeblog.service.EmailCodeService;
import com.animeblog.service.UserService;
import com.animeblog.util.ExpiringCache;
import com.animeblog.util.InputValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供用户认证相关功能,包括登录、注册、验证码、邮箱验证码、密码重置等
 * URL前缀: /api/auth
 * 访问权限: 公开接口,无需登录
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 密码规则提示信息
    private static final String PASSWORD_RULE_MESSAGE = "密码需为 8-20 位，且字母、数字、特殊字符至少满足两类";

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailCodeMapper emailCodeMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private TurnstileService turnstileService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    // 验证码缓存，使用带自动过期清理的 ExpiringCache
    // 清理间隔：60 秒，验证码有效期：5 分钟（由 CaptchaData 的 expireTime 控制）
    private static final ExpiringCache<CaptchaData> captchaCache = new ExpiringCache<>(60);

    // 验证码缓存最大数量限制（防止 DoS 攻击）
    private static final int MAX_CACHE_SIZE = 10000;

    // 验证码数据结构，存储验证码和过期时间
    static class CaptchaData {
        private final String code;
        private final long expireTime;

        CaptchaData(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }

    /**
     * 用户登录
     * URL: POST /api/auth/login
     * 参数: LoginRequest对象(包含username, password, turnstileToken, rememberMe是否记住我)
     * @return 登录结果(包含token等用户信息)
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String clientIp = resolveClientIp(httpServletRequest);

        if (turnstileService.isLoginProtectionEnabled()
                && loginAttemptService.isTurnstileRequired(username, clientIp, turnstileService.getLoginFailureThreshold())) {
            turnstileService.validateOrThrow(
                    request.getTurnstileToken(),
                    clientIp,
                    "login",
                    "登录失败次数较多，请完成人机验证后重试"
            );
        }

        Map<String, Object> result = userService.login(
                username,
                request.getPassword(),
                Boolean.TRUE.equals(request.getRememberMe())
        );
        if (result == null) {
            int failureCount = loginAttemptService.recordFailure(username, clientIp);
            if (turnstileService.isLoginProtectionEnabled()
                    && failureCount >= turnstileService.getLoginFailureThreshold()) {
                return buildTurnstileRequiredResponse(failureCount);
            }
            return ApiResponse.error("用户名或密码错误");
        }
        loginAttemptService.clearFailures(username, clientIp);
        return ApiResponse.success(result);
    }

    /**
     * 用户注册
     * URL: POST /api/auth/register
     * 参数: RegisterRequest对象(包含username, email, password, confirmPassword, emailCode邮箱验证码)
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        // 清理输入,去除首尾空格
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!InputValidationUtil.isUsernameValid(username)) {
            return ApiResponse.error("用户名长度应为 3-12 位");
        }
        if (password == null || confirmPassword == null) {
            return ApiResponse.error("请输入密码并确认密码");
        }
        if (!password.equals(confirmPassword)) {
            return ApiResponse.error("两次密码不一致");
        }
        if (!InputValidationUtil.isPasswordValid(password)) {
            return ApiResponse.error(PASSWORD_RULE_MESSAGE);
        }

        EmailCode emailCode = emailCodeMapper.selectLatestCode(email, "register");
        if (emailCode == null || !emailCode.getCode().equals(request.getEmailCode())) {
            return ApiResponse.error("邮箱验证码错误");
        }
        if (emailCode.getUsed() == 1) {
            return ApiResponse.error("邮箱验证码已使用");
        }
        emailCode.setUsed(1);
        emailCodeMapper.updateById(emailCode);

        boolean success = userService.register(username, email, password);
        if (!success) {
            return ApiResponse.error("用户名或邮箱已存在");
        }
        return ApiResponse.success();
    }

    @GetMapping("/turnstile/config")
    public ApiResponse<Map<String, Object>> getTurnstileConfig() {
        return ApiResponse.success(turnstileService.buildPublicConfig());
    }

    /**
     * 获取Base64编码的图形验证码(用于前端img标签直接展示)
     * URL: GET /api/auth/captcha/base64
     * 参数: type-验证码类型(login/register,默认login), identifier-标识符(可选,用于区分不同客户端)
     * @return 验证码图片(base64)及元信息
     */
    @GetMapping("/captcha/base64")
    public ApiResponse<Map<String, Object>> getCaptchaBase64(
            @RequestParam(defaultValue = "login") String type,
            @RequestParam(required = false) String identifier) {
        // 检查缓存是否已满（防止 DoS 攻击）
        if (isCacheFull()) {
            return ApiResponse.error("系统繁忙，请稍后重试");
        }

        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 5);
        String code = captcha.getCode();
        String base64Image = captcha.getImageBase64();

        captchaCache.put(
                buildCaptchaKey(type, identifier),
                new CaptchaData(code, System.currentTimeMillis() + 5 * 60 * 1000),
                5 * 60 * 1000 // 5 分钟后自动清理（毫秒）
        );

        Map<String, Object> result = new HashMap<>();
        result.put("image", base64Image);
        result.put("captchaImage", base64Image);
        result.put("captchaId", identifier == null || identifier.isBlank() ? "default" : identifier.trim());
        result.put("expire", 300);
        return ApiResponse.success(result);
    }

    /**
     * 获取图形验证码(直接返回图片流)
     * URL: GET /api/auth/captcha
     * 参数: type-验证码类型(login/register,默认login), identifier-标识符(可选)
     * 响应: 直接返回PNG图片流
     */
    @GetMapping("/captcha")
    public void getCaptcha(
            @RequestParam(defaultValue = "login") String type,
            @RequestParam(required = false) String identifier,
            HttpServletResponse response) {
        // 检查缓存是否已满（防止 DoS 攻击）
        if (isCacheFull()) {
            return;
        }

        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 5);
        captchaCache.put(
                buildCaptchaKey(type, identifier),
                new CaptchaData(captcha.getCode(), System.currentTimeMillis() + 5 * 60 * 1000),
                5 * 60 * 1000 // 5 分钟后自动清理（毫秒）
        );

        try {
            response.setContentType("image/png");
            captcha.write(response.getOutputStream());
        } catch (Exception ignored) {
        }
    }

    /**
     * 发送邮箱验证码(用于注册或换绑邮箱)
     * URL: POST /api/auth/send-email-code
     * 参数: Map对象(包含email-邮箱地址, type-类型register/change_email,默认register)
     * @return 发送结果
     */
    @PostMapping("/send-email-code")
    public ApiResponse<Map<String, Object>> sendEmailCode(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpServletRequest
    ) {
        String email = request.get("email");
        String type = request.getOrDefault("type", "register");
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ApiResponse.error("邮箱格式不正确");
        }
        if (!"register".equals(type) && !"change_email".equals(type)) {
            return ApiResponse.error("不支持的验证码类型");
        }

        if ("register".equals(type) && turnstileService.isRegisterEmailProtectionEnabled()) {
            turnstileService.validateOrThrow(
                    request.get("turnstileToken"),
                    resolveClientIp(httpServletRequest),
                    "register_email",
                    "请先完成人机验证"
            );
        }

        User existingUser = userMapper.selectByEmail(email);
        if (existingUser != null) {
            return ApiResponse.error("该邮箱已被注册");
        }

        emailCodeService.createAndSendCode(email, type);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "验证码已发送");
        result.put("expire", 300);
        return ApiResponse.success(result);
    }

    /**
     * 发送忘记密码验证码
     * URL: POST /api/auth/send-forget-code
     * 参数: Map对象(包含email-邮箱地址)
     * @return 发送结果
     */
    @PostMapping("/send-forget-code")
    public ApiResponse<Map<String, Object>> sendForgetCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return ApiResponse.error("邮箱格式不正确");
        }

        User existingUser = userMapper.selectByEmail(email);
        if (existingUser == null) {
            return ApiResponse.error("该邮箱未注册");
        }

        emailCodeService.createAndSendCode(email, "forget");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "验证码已发送");
        result.put("expire", 300);
        return ApiResponse.success(result);
    }

    /**
     * 验证邮箱验证码(主要用于忘记密码流程)
     * URL: POST /api/auth/verify-email-code
     * 参数: Map对象(包含email-邮箱地址, code-验证码, type-类型,默认forget)
     * @return 验证结果
     */
    @PostMapping("/verify-email-code")
    public ApiResponse<Void> verifyEmailCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String type = request.getOrDefault("type", "forget");
        if (email == null || code == null) {
            return ApiResponse.error("参数不完整");
        }

        EmailCode emailCode = emailCodeMapper.selectLatestCode(email, type);
        if (emailCode == null || !emailCode.getCode().equals(code)) {
            return ApiResponse.error("验证码错误");
        }
        return ApiResponse.success();
    }

    /**
     * 重置密码(忘记密码流程)
     * URL: POST /api/auth/reset-password
     * 参数: Map对象(包含email-邮箱地址, code-验证码, newPassword-新密码, confirmPassword-确认密码)
     * @return 重置结果
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        if (email == null || code == null || newPassword == null || confirmPassword == null) {
            return ApiResponse.error("参数不完整");
        }
        if (!InputValidationUtil.isPasswordValid(newPassword)) {
            return ApiResponse.error(PASSWORD_RULE_MESSAGE);
        }
        if (!newPassword.equals(confirmPassword)) {
            return ApiResponse.error("两次密码不一致");
        }

        EmailCode emailCode = emailCodeMapper.selectLatestCode(email, "forget");
        if (emailCode == null || !emailCode.getCode().equals(code)) {
            return ApiResponse.error("验证码错误或已过期");
        }
        if (emailCode.getUsed() == 1) {
            return ApiResponse.error("验证码已使用");
        }

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        emailCode.setUsed(1);
        emailCodeMapper.updateById(emailCode);
        return ApiResponse.success();
    }

    /**
     * 构建验证码缓存键
     */
    private String buildCaptchaKey(String type, String identifier) {
        String key = (identifier == null || identifier.isBlank()) ? "default" : identifier.trim();
        return type + ":" + key;
    }

    /**
     * 获取验证码数据(支持回退到default key)
     */
    private CaptchaData getCaptchaData(String type, String identifier) {
        CaptchaData captchaData = captchaCache.get(buildCaptchaKey(type, identifier));
        if (captchaData == null && identifier != null && !identifier.isBlank()) {
            captchaData = captchaCache.get(buildCaptchaKey(type, null));
        }
        return captchaData;
    }

    /**
     * 清除验证码缓存
     */
    private void clearCaptchaData(String type, String identifier) {
        captchaCache.remove(buildCaptchaKey(type, identifier));
        if (identifier != null && !identifier.isBlank()) {
            captchaCache.remove(buildCaptchaKey(type, null));
        }
    }

    /**
     * 检查缓存是否已满
     * 
     * @return true 如果缓存已满，false 否则
     */
    private boolean isCacheFull() {
        return captchaCache.size() >= MAX_CACHE_SIZE;
    }

    private ApiResponse<Map<String, Object>> buildTurnstileRequiredResponse(int failureCount) {
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setCode(429);
        response.setMessage("登录失败次数较多，请完成人机验证后重试");
        response.setData(Map.of(
                "requireTurnstile", true,
                "failureCount", failureCount,
                "failureThreshold", turnstileService.getLoginFailureThreshold()
        ));
        return response;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);
            if (headerValue == null || headerValue.isBlank() || "unknown".equalsIgnoreCase(headerValue)) {
                continue;
            }
            int separatorIndex = headerValue.indexOf(',');
            return separatorIndex >= 0 ? headerValue.substring(0, separatorIndex).trim() : headerValue.trim();
        }

        return request.getRemoteAddr();
    }
}
