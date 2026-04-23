package com.animeblog.security;

import com.animeblog.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * <p>
 * 职责：
 * 1. 从请求头中提取 JWT Token（Authorization: Bearer &lt;token&gt;）
 * 2. 验证 Token 的有效性（签名、过期时间）
 * 3. 解析 Token 中的用户信息并设置到 Spring Security 上下文
 * 4. 放行已认证的请求，未携带 Token 或 Token 无效则继续过滤链（由后续配置决定是否拦截）
 * </p>
 * 
 * <p>
 * 注意：该过滤器不会阻止请求继续执行，即使 Token 无效也会放行。
 * 是否拦截由 SecurityConfig 中的授权规则决定。
 * </p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头中提取 Token
        String token = extractToken(request);

        // 2. 如果 Token 存在且有效，则设置认证信息
        if (token != null && jwtUtil.validateAccessToken(token)) {
            try {
                // 3. 解析 Token 获取用户信息
                Claims claims = jwtUtil.parseToken(token);
                String username = claims.get("username", String.class);

                // 4. 加载用户详情
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. 创建认证对象并设置到 Spring Security 上下文
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // 已认证，不需要密码
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                logger.debug("成功认证用户: " + username);
            } catch (Exception e) {
                // Token 解析失败或用户加载失败，清空安全上下文
                logger.error("JWT 认证失败: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 6. 继续执行过滤链（无论是否认证成功）
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取 JWT Token
     * 
     * @param request HTTP 请求
     * @return Token 字符串，如果不存在则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
