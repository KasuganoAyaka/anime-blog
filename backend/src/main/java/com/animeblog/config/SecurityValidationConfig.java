package com.animeblog.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 安全配置验证类
 * 在应用启动时验证关键安全配置,防止使用不安全的默认值
 */
@Slf4j
@Configuration
public class SecurityValidationConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.mail.password}")
    private String mailPassword;

    /**
     * 启动时验证安全配置
     * 如果检测到不安全的配置,直接阻止应用启动
     */
    @PostConstruct
    public void validateSecurityConfig() {
        log.info("开始验证安全配置...");

        // 验证JWT密钥
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "安全配置错误: JWT密钥未设置! " +
                "请通过环境变量 ANIME_BLOG_JWT_SECRET 配置"
            );
        }

        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "安全配置错误: JWT密钥长度不足! " +
                "密钥长度至少32字节(256位),当前长度: " + jwtSecret.length() + " 字节\n" +
                "生成强密钥命令: openssl rand -base64 32"
            );
        }

        log.info("✓ JWT密钥验证通过 (长度: {} 字节)", jwtSecret.length());

        // 验证数据库密码
        if (dbPassword == null || dbPassword.trim().isEmpty()) {
            throw new IllegalStateException(
                "安全配置错误: 数据库密码未设置! " +
                "请通过环境变量 DB_PASSWORD 配置"
            );
        }

        // 警告使用弱密码
        String[] weakPasswords = {"root123", "123456", "password", "admin123"};
        for (String weak : weakPasswords) {
            if (dbPassword.equalsIgnoreCase(weak)) {
                log.error("⚠ 警告: 数据库密码过于简单! 建议修改为强密码");
                break;
            }
        }

        log.info("✓ 数据库密码验证通过");

        // 验证邮件密码
        if (mailPassword == null || mailPassword.trim().isEmpty()) {
            log.warn("⚠ 警告: 邮件服务器密码未设置,邮件功能将不可用");
        } else {
            log.info("✓ 邮件服务器密码验证通过");
        }

        log.info("安全配置验证完成!");
    }
}
