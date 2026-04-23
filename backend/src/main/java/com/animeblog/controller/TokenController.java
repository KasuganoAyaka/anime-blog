package com.animeblog.controller;

import com.animeblog.dto.ApiResponse;
import com.animeblog.dto.RefreshTokenRequest;
import com.animeblog.entity.User;
import com.animeblog.service.RefreshTokenService;
import com.animeblog.util.AuthHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Token 管理控制器
 * <p>
 * 提供 Token 刷新、会话管理、退出登录等功能
 * URL 前缀: /api/auth/tokens
 * 访问权限: 需要登录
 * </p>
 */
@RestController
@RequestMapping("/api/auth/tokens")
public class TokenController {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AuthHelper authHelper;

    /**
     * 刷新 Access Token
     * <p>
     * 使用 Refresh Token 获取新的 Access Token
     * 无需携带 Authorization 请求头，仅需 Refresh Token
     * </p>
     * 
     * URL: POST /api/auth/tokens/refresh
     * 权限: 公开（仅需有效的 Refresh Token）
     * 
     * @param request 刷新令牌请求（包含 refreshToken）
     * @return 新的 Access Token 和用户信息
     */
    @PostMapping("/refresh")
    public ApiResponse<?> refreshAccessToken(@RequestBody RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return ApiResponse.error("Refresh Token 不能为空");
        }

        Map<String, Object> result = refreshTokenService.refreshAccessToken(request.getRefreshToken());
        return ApiResponse.success(result);
    }

    /**
     * 退出登录（撤销当前 Refresh Token）
     * 
     * URL: POST /api/auth/tokens/logout
     * 权限: 需要登录
     * 
     * @param authorization Authorization 请求头
     * @param request HTTP 请求（用于获取 Refresh Token）
     * @return 退出结果
     */
    @PostMapping("/logout")
    public ApiResponse<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录");
        }

        // 从请求头获取 Refresh Token（通常由前端在 Logout 时传递）
        String refreshToken = request.getHeader("X-Refresh-Token");
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeRefreshToken(refreshToken);
        } else {
            // 如果没有传递 Refresh Token，撤销该用户的所有 Token
            refreshTokenService.revokeAllUserTokens(currentUser.getId());
        }

        return ApiResponse.success("退出登录成功");
    }

    /**
     * 获取用户的所有有效会话（设备管理）
     * 
     * URL: GET /api/auth/tokens/sessions
     * 权限: 需要登录
     * 
     * @param authorization Authorization 请求头
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public ApiResponse<?> getUserSessions(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录");
        }

        List<Map<String, Object>> sessions = refreshTokenService.getUserSessions(currentUser.getId());
        return ApiResponse.success(sessions);
    }

    /**
     * 撤销指定会话（远程退出登录）
     * 
     * URL: DELETE /api/auth/tokens/sessions/{jti}
     * 权限: 需要登录（仅能撤销自己的会话）
     * 
     * @param authorization Authorization 请求头
     * @param jti JWT ID
     * @return 撤销结果
     */
    @DeleteMapping("/sessions/{jti}")
    public ApiResponse<?> revokeSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String jti) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录");
        }

        refreshTokenService.revokeSession(jti, currentUser.getId());
        return ApiResponse.success("会话已撤销");
    }

    /**
     * 退出所有设备（撤销所有会话）
     * 
     * URL: POST /api/auth/tokens/logout-all
     * 权限: 需要登录
     * 
     * @param authorization Authorization 请求头
     * @return 退出结果
     */
    @PostMapping("/logout-all")
    public ApiResponse<?> logoutAllDevices(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录");
        }

        refreshTokenService.revokeAllUserTokens(currentUser.getId());
        return ApiResponse.success("已退出所有设备");
    }
}
