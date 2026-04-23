package com.animeblog.util;

import com.animeblog.entity.User;
import com.animeblog.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 认证辅助工具类
 * 提供用户身份验证、权限判断等功能,基于JWT Token实现
 */
@Component
public class AuthHelper {
    /** 站长角色标识 */
    public static final String ROLE_STATION_MASTER = "admin";
    /** 管理员角色标识 */
    public static final String ROLE_MANAGER = "manager";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    /**
     * 根据Authorization请求头获取当前登录用户信息
     *
     * @param authorization 请求头中的Authorization字段值,格式为"Bearer {token}"或直接传token
     * @return 用户信息对象,如果token无效、解析失败或用户状态异常则返回null
     */
    public User getCurrentUser(String authorization) {
        // 检查authorization是否为空
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        // 提取token:如果以"Bearer "开头则截取前缀后的部分,否则直接使用
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;
        if (token.isBlank()) {
            return null;
        }

        try {
            // 解析JWT Token获取Claims
            Claims claims = jwtUtil.parseToken(token);
            // 从Claims的subject字段获取用户ID
            Long userId = Long.parseLong(claims.getSubject());
            // 根据用户ID查询数据库获取用户信息
            User user = userMapper.selectById(userId);
            // 检查用户是否存在且状态正常(status=1表示启用)
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                return null;
            }
            return user;
        } catch (Exception ignored) {
            // Token解析失败或用户查询异常时返回null
            return null;
        }
    }

    /**
     * 判断用户是否为管理员(站长或管理员角色)
     *
     * @param user 用户对象
     * @return 如果用户不为空且角色为admin或manager则返回true
     */
    public boolean isAdmin(User user) {
        return user != null && (
                ROLE_STATION_MASTER.equals(user.getRole())
                        || ROLE_MANAGER.equals(user.getRole())
        );
    }

    /**
     * 判断用户是否为站长(超级管理员)
     * 需要同时满足:角色为admin且用户名为"admin"
     *
     * @param user 用户对象
     * @return 如果用户不为空、角色为admin且用户名为"admin"则返回true
     */
    public boolean isStationMaster(User user) {
        return user != null
                && ROLE_STATION_MASTER.equals(user.getRole())
                && "admin".equals(user.getUsername());
    }

    /**
     * 判断用户是否为普通管理员
     *
     * @param user 用户对象
     * @return 如果用户不为空且角色为manager则返回true
     */
    public boolean isManager(User user) {
        return user != null && ROLE_MANAGER.equals(user.getRole());
    }
}
