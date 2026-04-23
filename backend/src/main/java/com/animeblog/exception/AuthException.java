package com.animeblog.exception;

/**
 * 认证异常
 * 用于处理用户未登录、token无效、token过期等认证相关错误
 */
public class AuthException extends RuntimeException {
    
    /**
     * 创建认证异常
     * @param message 错误消息
     */
    public AuthException(String message) {
        super(message);
    }

    /**
     * 创建认证异常(带cause)
     * @param message 错误消息
     * @param cause 原始异常
     */
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
