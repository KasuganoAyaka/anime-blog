package com.animeblog.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 提供Token生成、解析和验证功能,用于用户身份认证
 * 
 * 支持两种 Token 类型：
 * - Access Token：短期有效（2小时），用于 API 认证
 * - Refresh Token：长期有效（30天），用于刷新 Access Token
 */
@Component
public class JwtUtil {
    /** JWT签名密钥,从配置文件读取 */
    @Value("${jwt.secret}")
    private String secret;

    /** Access Token 过期时间(秒),默认2小时(7200秒) */
    @Value("${jwt.expire:7200}")
    private long accessExpireSeconds;

    /**
     * 根据签名密钥生成HMAC-SHA加密密钥对象
     * 使用UTF-8编码确保跨平台一致性
     *
     * @return SecretKey加密密钥对象
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token（短期有效，用于 API 认证）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return Access Token 字符串
     */
    public String generateAccessToken(Long userId, String username) {
        return generateAccessToken(userId, username, accessExpireSeconds);
    }

    /**
     * 生成 Access Token（可自定义过期时间）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param expireSeconds 过期时间（秒）
     * @return Access Token 字符串
     */
    public String generateAccessToken(Long userId, String username, long expireSeconds) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access"); // Token 类型声明
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireSeconds * 1000))
                .signWith(getKey())
                .compact();
    }

    /**
     * 生成 Refresh Token（长期有效，用于刷新 Access Token）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param jti JWT ID（唯一标识符）
     * @param expireSeconds 过期时间（秒）
     * @return Refresh Token 字符串
     */
    public String generateRefreshToken(Long userId, String username, String jti, long expireSeconds) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh"); // Token 类型声明
        claims.put("username", username);
        claims.put("jti", jti); // JWT ID，用于唯一标识

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireSeconds * 1000))
                .signWith(getKey())
                .compact();
    }

    /**
     * 生成JWT Token（旧版本，兼容现有代码，生成 Access Token）
     *
     * @param userId 用户ID,存储在token的subject字段
     * @param username 用户名,存储在token的自定义username字段
     * @return 生成的JWT Token字符串
     * @deprecated 请使用 generateAccessToken 方法
     */
    @Deprecated
    public String generateToken(Long userId, String username) {
        return generateAccessToken(userId, username);
    }

    /**
     * 生成JWT Token（旧版本，可自定义过期时间）
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param customExpireSeconds 自定义过期时间(秒)
     * @return 生成的JWT Token字符串
     * @deprecated 请使用 generateAccessToken 方法
     */
    @Deprecated
    public String generateToken(Long userId, String username, long customExpireSeconds) {
        return generateAccessToken(userId, username, customExpireSeconds);
    }

    /**
     * 解析JWT Token获取声明信息
     *
     * @param token JWT Token字符串
     * @return Claims声明对象,包含用户信息和token元数据
     * @throws io.jsonwebtoken.JwtException 如果token无效、过期或签名不匹配则抛出异常
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证JWT Token是否有效（不区分类型）
     *
     * @param token JWT Token字符串
     * @return 如果token解析成功(签名正确且未过期)返回true,否则返回false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 验证 Access Token 是否有效
     *
     * @param token JWT Token字符串
     * @return true 如果是有效的 Access Token，false 否则
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "access".equals(claims.get("type", String.class));
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 验证 Refresh Token 是否有效
     *
     * @param token JWT Token字符串
     * @return true 如果是有效的 Refresh Token，false 否则
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception ignored) {
            return false;
        }
    }
}
