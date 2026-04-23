package com.animeblog.dto;

import lombok.Data;

/**
 * 统一API响应封装类
 * 用于包装所有API的返回数据,提供统一的状态码和消息格式
 * @param <T> 返回数据类型
 */
@Data
public class ApiResponse<T> {
    
    /** HTTP状态码或业务状态码 */
    private int code;
    
    /** 响应消息 */
    private String message;
    
    /** 响应数据 */
    private T data;

    /**
     * 创建成功响应
     * @param data 返回数据
     * @return 成功响应对象
     * @param <T> 数据类型
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    /**
     * 创建成功响应(无数据)
     * @return 成功响应对象
     * @param <T> 数据类型
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 创建错误响应
     * @param message 错误消息
     * @return 错误响应对象
     * @param <T> 数据类型
     */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建错误响应(自定义错误码)
     * @param message 错误消息
     * @param code 错误码
     * @return 错误响应对象
     * @param <T> 数据类型
     */
    public static <T> ApiResponse<T> error(String message, int code) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建未授权响应(401)
     * @param message 错误消息
     * @return 未授权响应对象
     * @param <T> 数据类型
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(401);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建禁止访问响应(403)
     * @param message 错误消息
     * @return 禁止访问响应对象
     * @param <T> 数据类型
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(403);
        response.setMessage(message);
        return response;
    }
}
