package com.animeblog.service;

import com.animeblog.entity.User;
import com.animeblog.mapper.UserMapper;
import com.animeblog.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务
 * 处理用户登录、注册等核心用户管理功能
 * 使用 JWT 进行身份验证，支持 Refresh Token 机制实现长期登录
 */
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    /**
     * 用户登录(默认2小时有效期)
     *
     * @param username 用户名或邮箱
     * @param password 明文密码
     * @return 登录结果(包含token和用户信息),登录失败返回null
     */
    public Map<String, Object> login(String username, String password) {
        return login(username, password, false);
    }

    /**
     * 用户登录
     * 验证用户名/邮箱和密码,成功后生成 Access Token 和 Refresh Token
     *
     * @param username  用户名或邮箱
     * @param password  明文密码
     * @param rememberMe 是否记住我(使用长期 Refresh Token)
     * @return 登录结果(包含 accessToken, refreshToken 和用户信息),登录失败返回null
     */
    public Map<String, Object> login(String username, String password, boolean rememberMe) {
        // 查询用户(支持用户名或邮箱登录)
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).or().eq("email", username);
        User user = userMapper.selectOne(wrapper);

        // 验证用户存在且密码正确
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        // 验证用户状态为活跃
        if (user.getStatus() == null || user.getStatus() != 1) {
            return null;
        }

        // 生成 Access Token（短期，2小时）
        long accessExpireSeconds = 2 * 60 * 60L; // 2 小时
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), accessExpireSeconds);

        // 获取请求信息（用于记录设备和 IP）
        String deviceInfo = "Unknown";
        String ipAddress = "Unknown";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String userAgent = request.getHeader("User-Agent");
                deviceInfo = userAgent != null ? userAgent : "Unknown";
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            // 忽略异常，使用默认值
        }

        // 生成 Refresh Token（长期，30天或7天）
        Map<String, Object> refreshTokenData = refreshTokenService.createRefreshToken(
                user, deviceInfo, ipAddress);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshTokenData.get("refreshToken"));
        result.put("expiresIn", accessExpireSeconds);
        result.put("refreshExpiresIn", refreshTokenData.get("expiresIn"));

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("nickname", user.getNickname());
        userData.put("avatar", user.getAvatar());
        userData.put("role", user.getRole());
        result.put("user", userData);

        return result;
    }

    /**
     * 用户注册
     * 检查用户名和邮箱唯一性,密码加密存储
     * 注册成功后自动设置为已验证邮箱和活跃状态
     *
     * @param username 用户名
     * @param email    邮箱
     * @param password 明文密码
     * @return 注册是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean register(String username, String email, String password) {
        username = username == null ? null : username.trim();
        email = email == null ? null : email.trim();

        // 检查用户名或邮箱是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username).or().eq("email", email);
        if (userMapper.selectCount(wrapper) > 0) {
            return false;
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // BCrypt加密
        user.setNickname(username); // 默认昵称为用户名
        user.setRole("user");       // 默认角色
        user.setStatus(1);          // 活跃状态
        user.setEmailVerified(1);   // 邮箱已验证

        return userMapper.insert(user) > 0;
    }

    /**
     * 根据ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户实体,不存在时返回null
     */
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
