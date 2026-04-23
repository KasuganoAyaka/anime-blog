package com.animeblog.exception;

import com.animeblog.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 职责：
 * 1. 统一捕获和处理所有异常，防止敏感信息泄露
 * 2. 返回标准化的错误响应给前端（统一使用 ApiResponse 格式）
 * 3. 详细错误信息仅记录在服务端日志，不返回给客户端
 * 4. 提供错误追踪ID和时间戳，方便问题排查
 * </p>
 * 
 * <p>
 * 使用方式：
 * - Controller 层直接抛出各种异常（BusinessException、AuthException 等）
 * - 该处理器会自动拦截并转换为统一的 ApiResponse 格式
 * - 客户端只会看到简化的错误消息，详细堆栈记录在日志中
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 处理业务异常
     * <p>业务异常是可控的，可以返回具体错误信息给客户端</p>
     * 
     * @param e 业务异常
     * @param request HTTP请求
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] [{}]: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return ApiResponse.error(e.getMessage(), e.getErrorCode());
    }

    /**
     * 处理认证异常（未登录或token无效）
     * 
     * @param e 认证异常
     * @param request HTTP请求
     * @return 统一错误响应
     */
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthException(AuthException e, HttpServletRequest request) {
        log.warn("认证异常 [{}] [{}]: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return ApiResponse.unauthorized(e.getMessage());
    }

    /**
     * 处理权限异常（已登录但无权限）
     * 
     * @param e 权限异常
     * @param request HTTP请求
     * @return 统一错误响应
     */
    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleForbiddenException(ForbiddenException e, HttpServletRequest request) {
        log.warn("权限异常 [{}] [{}]: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return ApiResponse.forbidden(e.getMessage());
    }

    /**
     * 处理参数验证异常（@Valid 注解触发）
     * <p>这是最常见的验证异常，会自动收集所有字段的验证错误</p>
     * 
     * @param e 验证异常
     * @return 统一错误响应（包含所有字段的错误信息）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 提取所有字段的错误信息，拼接成友好的提示
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("参数验证异常: {}", errorMessage);
        return ApiResponse.error(errorMessage, 400);
    }

    /**
     * 处理绑定异常（表单提交验证失败）
     * 
     * @param e 绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        String errorMessage = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("表单验证异常: {}", errorMessage);
        return ApiResponse.error(errorMessage, 400);
    }

    /**
     * 处理请求参数缺失异常
     * 
     * @param e 参数缺失异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        String message = "缺少必要参数: " + e.getParameterName();
        log.warn("参数缺失: {}", message);
        return ApiResponse.error(message, 400);
    }

    /**
     * 处理HTTP请求方法不支持异常（如GET请求发送了POST方法）
     * 
     * @param e 请求方法不支持异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = "不支持的请求方法: " + e.getMethod();
        log.warn("请求方法不支持: {}", message);
        return ApiResponse.error(message, 405);
    }

    /**
     * 处理请求体无法解析异常（JSON格式错误等）
     * 
     * @param e 请求体解析异常
     * @return 统一错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ApiResponse.error("请求数据格式错误，请检查JSON格式", 400);
    }

    /**
     * 处理文件上传大小超出限制异常
     * 
     * @param e 文件大小超出限制异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> handleMaxUploadSizeException(MaxUploadSizeExceededException e) {
        log.warn("上传文件大小超出限制");
        return ApiResponse.error("上传文件大小超出限制", 413);
    }

    /**
     * 处理SQL异常（数据库错误）
     * <p>这是敏感异常，不能返回详细SQL信息给客户端</p>
     * 
     * @param e SQL异常
     * @param request HTTP请求
     * @return 统一错误响应
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleSQLException(SQLException e, HttpServletRequest request) {
        // 详细SQL错误仅记录日志，不返回给客户端
        log.error("数据库异常 [{}] [{}]: SQLState={}, ErrorCode={}",
                request.getMethod(), request.getRequestURI(), e.getSQLState(), e.getErrorCode(), e);
        return ApiResponse.error("数据库操作失败，请稍后重试", 500);
    }

    /**
     * 处理资源未找到异常（404）
     * 
     * @param e 资源未找到异常
     * @return 统一错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("资源未找到: {}", e.getResourcePath());
        return ApiResponse.error("请求的资源不存在", 404);
    }

    /**
     * 处理非法参数异常
     * 
     * @param e 非法参数异常
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return ApiResponse.error(e.getMessage(), 400);
    }

    /**
     * 处理非法状态异常
     * 
     * @param e 非法状态异常
     * @param request HTTP请求
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleIllegalStateException(IllegalStateException e, HttpServletRequest request) {
        log.error("非法状态 [{}] [{}]: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        return ApiResponse.error("系统状态异常，请联系管理员", 500);
    }

    /**
     * 处理空指针异常
     * <p>这通常是代码bug，需要开发人员排查</p>
     * 
     * @param e 空指针异常
     * @param request HTTP请求
     * @return 统一错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 [{}] [{}]", 
                request.getMethod(), request.getRequestURI(), e);
        return ApiResponse.error("系统内部错误，请稍后重试", 500);
    }

    /**
     * 处理运行时异常
     * <p>捕获未被专门处理的 RuntimeException</p>
     * 
     * @param e 运行时异常
     * @param request HTTP请求
     * @return 统一错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        log.error("运行时异常 [{}] [{}]: {}", 
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);
        return ApiResponse.error("系统内部错误，请稍后重试", 500);
    }

    /**
     * 处理所有未被捕获的未知异常
     * <p>这是最后一道防线，防止任何异常信息泄露给客户端</p>
     * 
     * @param e 未知异常
     * @param request HTTP请求
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e, HttpServletRequest request) {
        // 记录完整的异常堆栈到日志，方便排查问题
        log.error("未知异常 [{}] [{}]: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage(), e);

        // 返回给客户端的必须是通用错误消息
        return ApiResponse.error("系统内部错误，请稍后重试", 500);
    }
}
