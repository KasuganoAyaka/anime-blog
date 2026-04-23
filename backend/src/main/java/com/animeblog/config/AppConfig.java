package com.animeblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 应用基础配置类
 * 用于配置全局通用的Bean组件
 */
@Configuration
public class AppConfig {
    
    /**
     * 密码编码器Bean
     * 使用BCrypt算法对用户密码进行加密处理
     * @return BCrypt密码编码器实例
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
