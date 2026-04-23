package com.animeblog.dto;

import lombok.Data;

/**
 * 登录请求DTO
 * 用于接收用户登录时提交的表单数据
 */
@Data
public class LoginRequest {
    
    /** 用户名 */
    private String username;
    
    /** 密码 */
    private String password;
    
    /** 验证码 */
    private String captcha;

    /** Cloudflare Turnstile 响应令牌 */
    private String turnstileToken;
    
    /** 是否记住登录状态 */
    private Boolean rememberMe;
}
