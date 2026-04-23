package com.animeblog.util;

import com.animeblog.entity.User;
import com.animeblog.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Spring Security 辅助工具类
 * <p>
 * 职责：
 * 1. 从 Spring Security 上下文中获取当前认证用户
 * 2. 提供与原有 AuthHelper 兼容的接口
 * 3. 简化 Controller 层的用户信息获取方式
 * </p>
 * 
 * <p>
 * 使用方式：
 * <pre>
 * // 在 Controller 中
 * User currentUser = securityHelper.getCurrentUser();
 * if (currentUser == null) {
 *     throw new AuthException("未登录");
 * }
 * </pre>
 * </p>
 */
@Component
public class SecurityHelper {

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取当前登录用户信息
     * <p>
     * 从 Spring Security 上下文中获取认证用户名，然后查询数据库获取完整用户信息。
     * </p>
     * 
     * @return 用户信息对象，如果未认证则返回 null
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // 获取认证的用户名
        String username = null;
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            return null;
        }

        // 查询数据库获取用户信息
        try {
            return userMapper.selectByUsername(username);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前登录用户信息（兼容旧接口）
     * <p>
     * 为了兼容原有代码，支持从 Authorization 请求头获取用户。
     * 如果 Spring Security 上下文中有用户，则优先使用；否则回退到 AuthHelper。
     * </p>
     * 
     * @param authorization Authorization 请求头（可选，用于兼容）
     * @return 用户信息对象，如果未认证则返回 null
     * @deprecated 建议直接使用 getCurrentUser() 方法
     */
    @Deprecated
    public User getCurrentUser(String authorization) {
        // 优先使用 Spring Security 上下文
        User user = getCurrentUser();
        if (user != null) {
            return user;
        }
        
        // 回退：这个情况不应该发生，因为 SecurityConfig 已经配置了 JWT 过滤器
        return null;
    }

    /**
     * 判断用户是否已认证（已登录）
     * 
     * @return true 如果已认证，false 否则
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null 
                && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String);
    }

    /**
     * 判断当前用户是否具有指定角色
     * 
     * @param role 角色名称（如 "admin", "manager", "user"）
     * @return true 如果具有该角色，false 否则
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String expectedRole = "ROLE_" + role.toUpperCase();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(expectedRole));
    }

    /**
     * 判断当前用户是否为管理员（站长或管理员）
     * 
     * @return true 如果是管理员，false 否则
     */
    public boolean isAdmin() {
        return hasRole("admin") || hasRole("manager");
    }

    /**
     * 判断当前用户是否为站长
     * 
     * @return true 如果是站长，false 否则
     */
    public boolean isStationMaster() {
        User user = getCurrentUser();
        return user != null 
                && "admin".equals(user.getRole())
                && "admin".equals(user.getUsername());
    }
}
