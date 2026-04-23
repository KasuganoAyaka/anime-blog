package com.animeblog.service;

import com.animeblog.entity.RefreshToken;
import com.animeblog.entity.User;
import com.animeblog.exception.BusinessException;
import com.animeblog.mapper.RefreshTokenMapper;
import com.animeblog.mapper.UserMapper;
import com.animeblog.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 刷新令牌服务
 * <p>
 * 职责：
 * 1. 生成和管理 Refresh Token
 * 2. 验证 Refresh Token 的有效性
 * 3. 使用 Refresh Token 刷新 Access Token
 * 4. 撤销 Refresh Token（退出登录、设备管理等）
 * 5. 清理过期的 Refresh Token
 * </p>
 * 
 * <p>
 * Token 类型说明：
 * - Access Token：短期有效（2小时），用于 API 认证
 * - Refresh Token：长期有效（30天），用于刷新 Access Token
 * </p>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    /** Refresh Token 过期时间（秒），默认 30 天 */
    @Value("${jwt.refresh-expire:2592000}")
    private long refreshExpireSeconds;

    /**
     * 创建 Refresh Token
     * 
     * @param user 用户实体
     * @param deviceInfo 设备信息（可选）
     * @param ipAddress IP 地址（可选）
     * @return 包含 Token 信息的 Map
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createRefreshToken(User user, String deviceInfo, String ipAddress) {
        // 1. 生成 JTI（唯一标识符）
        String jti = UUID.randomUUID().toString();

        // 2. 生成 Refresh Token（JWT 格式）
        String token = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), jti, refreshExpireSeconds);

        // 3. 构建 Refresh Token 实体
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setToken(token);
        refreshToken.setJti(jti);
        refreshToken.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
        refreshToken.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
        refreshToken.setExpireTime(LocalDateTime.now().plusSeconds(refreshExpireSeconds));
        refreshToken.setRevoked(0);
        refreshToken.setCreateTime(LocalDateTime.now());
        refreshToken.setUpdateTime(LocalDateTime.now());

        // 4. 保存到数据库
        refreshTokenMapper.insert(refreshToken);

        log.info("创建 Refresh Token: userId={}, jti={}", user.getId(), jti);

        // 5. 返回 Token 信息
        Map<String, Object> result = new HashMap<>();
        result.put("refreshToken", token);
        result.put("jti", jti);
        result.put("expiresIn", refreshExpireSeconds);
        return result;
    }

    /**
     * 使用 Refresh Token 刷新 Access Token
     * 
     * @param refreshToken Refresh Token 字符串
     * @return 包含新 Access Token 的 Map
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        // 1. 验证 Refresh Token 格式
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (Exception e) {
            throw new BusinessException("Refresh Token 无效或已过期");
        }

        // 2. 验证 Token 类型
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException("无效的 Token 类型");
        }

        // 3. 获取 JTI 和用户 ID
        String jti = claims.get("jti", String.class);
        Long userId = Long.parseLong(claims.getSubject());

        // 4. 查询数据库中的 Refresh Token
        RefreshToken storedToken = refreshTokenMapper.selectByJti(jti);
        if (storedToken == null) {
            throw new BusinessException("Refresh Token 不存在或已被撤销");
        }

        // 5. 验证 Token 是否匹配
        if (!storedToken.getToken().equals(refreshToken)) {
            // Token 不匹配，可能存在泄露，撤销所有该用户的 Token
            revokeAllUserTokens(userId);
            throw new BusinessException("Refresh Token 无效，已撤销所有登录会话");
        }

        // 6. 验证 Token 是否有效（未过期、未撤销）
        if (!storedToken.isValid()) {
            throw new BusinessException("Refresh Token 已过期或已被撤销");
        }

        // 7. 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("用户不存在或已被禁用");
        }

        // 8. 生成新的 Access Token
        long accessExpireSeconds = 2 * 60 * 60L; // 2 小时
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), accessExpireSeconds);

        // 9. 可选：轮换 Refresh Token（生成新的 Refresh Token，增强安全性）
        // 这里选择不轮换，保持原有 Refresh Token 不变
        // 如果需要轮换，可以在此处撤销旧的并创建新的

        log.info("刷新 Access Token: userId={}, jti={}", userId, jti);

        // 10. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("expiresIn", accessExpireSeconds);
        result.put("user", buildUserData(user));
        return result;
    }

    /**
     * 撤销 Refresh Token（退出登录）
     * 
     * @param refreshToken Refresh Token 字符串
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeRefreshToken(String refreshToken) {
        RefreshToken token = refreshTokenMapper.selectByToken(refreshToken);
        if (token != null && !token.isRevokedFlag()) {
            token.setRevoked(1);
            token.setUpdateTime(LocalDateTime.now());
            refreshTokenMapper.updateById(token);
            log.info("撤销 Refresh Token: userId={}, jti={}", token.getUserId(), token.getJti());
        }
    }

    /**
     * 撤销用户的所有 Refresh Token（密码修改、安全事件等）
     * 
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeAllUserTokens(Long userId) {
        List<RefreshToken> tokens = refreshTokenMapper.selectValidByUserId(userId);
        for (RefreshToken token : tokens) {
            token.setRevoked(1);
            token.setUpdateTime(LocalDateTime.now());
            refreshTokenMapper.updateById(token);
        }
        log.info("撤销用户所有 Refresh Token: userId={}, count={}", userId, tokens.size());
    }

    /**
     * 获取用户的所有有效会话（用于设备管理）
     * 
     * @param userId 用户 ID
     * @return 会话列表
     */
    public List<Map<String, Object>> getUserSessions(Long userId) {
        List<RefreshToken> tokens = refreshTokenMapper.selectValidByUserId(userId);
        return tokens.stream().map(token -> {
            Map<String, Object> session = new HashMap<>();
            session.put("jti", token.getJti());
            session.put("deviceInfo", token.getDeviceInfo());
            session.put("ipAddress", token.getIpAddress());
            session.put("createTime", token.getCreateTime());
            session.put("expireTime", token.getExpireTime());
            return session;
        }).toList();
    }

    /**
     * 撤销指定会话
     * 
     * @param jti JWT ID
     * @param userId 用户 ID（用于验证权限）
     */
    @Transactional(rollbackFor = Exception.class)
    public void revokeSession(String jti, Long userId) {
        RefreshToken token = refreshTokenMapper.selectByJti(jti);
        if (token != null && token.getUserId().equals(userId)) {
            token.setRevoked(1);
            token.setUpdateTime(LocalDateTime.now());
            refreshTokenMapper.updateById(token);
            log.info("撤销会话: jti={}, userId={}", jti, userId);
        }
    }

    /**
     * 清理所有过期的 Refresh Token（定时任务调用）
     * 
     * @return 清理的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupExpiredTokens() {
        List<RefreshToken> expiredTokens = refreshTokenMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<RefreshToken>lambdaQuery()
                        .lt(RefreshToken::getExpireTime, LocalDateTime.now())
                        .or()
                        .eq(RefreshToken::getRevoked, 1)
        );

        int count = expiredTokens.size();
        for (RefreshToken token : expiredTokens) {
            refreshTokenMapper.deleteById(token.getId());
        }

        if (count > 0) {
            log.info("清理过期/已撤销 Refresh Token: count={}", count);
        }
        return count;
    }

    /**
     * 构建用户数据（返回给前端）
     */
    private Map<String, Object> buildUserData(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("nickname", user.getNickname());
        userData.put("avatar", user.getAvatar());
        userData.put("role", user.getRole());
        return userData;
    }
}
