package com.animeblog.exception;

import lombok.Getter;

/**
 * 业务异常
 * 用于处理业务逻辑错误,如数据不存在、参数错误等
 * 这类异常会返回具体错误信息给客户端
 */
@Getter
public class BusinessException extends RuntimeException {
    
    /** 错误码 */
    private final Integer errorCode;

    /**
     * 创建业务异常
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = 400;
    }

    /**
     * 创建业务异常(自定义错误码)
     * @param message 错误消息
     * @param errorCode 错误码
     */
    public BusinessException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建业务异常(带cause)
     * @param message 错误消息
     * @param cause 原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 400;
    }
}
