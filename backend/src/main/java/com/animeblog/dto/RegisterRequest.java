package com.animeblog.dto;

import lombok.Data;

/**
 * 注册请求DTO
 * 用于接收用户注册时提交的表单数据
 */
@Data
public class RegisterRequest {
    
    /** 用户名 */
    private String username;
    
    /** 邮箱地址 */
    private String email;
    
    /** 密码 */
    private String password;
    
    /** 确认密码 */
    private String confirmPassword;
    
    /** 邮箱验证码 */
    private String emailCode;
    
    /** 图形验证码 */
    private String captchaCode;
}
