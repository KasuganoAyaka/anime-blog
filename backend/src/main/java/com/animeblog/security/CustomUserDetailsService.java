package com.animeblog.security;

import com.animeblog.entity.User;
import com.animeblog.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户详情服务
 * <p>
 * 职责：
 * 1. 实现 Spring Security 的 UserDetailsService 接口
 * 2. 从数据库加载用户信息并转换为 Spring Security 的 UserDetails 对象
 * 3. 将用户的角色转换为 Spring Security 的权限体系
 * </p>
 * 
 * <p>
 * 使用场景：
 * - JWT 过滤器解析 Token 后，通过用户名加载完整的用户详情
 * - Spring Security 认证流程中验证用户是否存在
 * </p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 根据用户名加载用户详情
     * 
     * @param username 用户名
     * @return Spring Security 的 UserDetails 对象
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 从数据库查询用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 2. 检查用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 3. 构建用户权限列表
        List<SimpleGrantedAuthority> authorities = buildAuthorities(user);

        // 4. 返回 Spring Security 的 UserDetails 对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    /**
     * 根据用户角色构建权限列表
     * 
     * @param user 用户实体
     * @return 权限列表
     */
    private List<SimpleGrantedAuthority> buildAuthorities(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 添加角色（Spring Security 要求角色以 ROLE_ 开头）
        if (user.getRole() != null && !user.getRole().isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
        }

        // 可以在此处添加更多权限（如菜单权限、操作权限等）
        // 例如：authorities.add(new SimpleGrantedAuthority("user:read"));

        return authorities;
    }
}
