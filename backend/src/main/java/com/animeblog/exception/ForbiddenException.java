package com.animeblog.exception;

/**
 * 权限异常
 * 用于处理用户已登录但无权限访问某资源的情况
 */
public class ForbiddenException extends RuntimeException {
    
    /**
     * 创建权限异常
     * @param message 错误消息
     */
    public ForbiddenException(String message) {
        super(message);
    }

    /**
     * 创建权限异常(带cause)
     * @param message 错误消息
     * @param cause 原始异常
     */
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
