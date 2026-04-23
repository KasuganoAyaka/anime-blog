package com.animeblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 博客应用启动类
 * Spring Boot应用入口,配置组件扫描和Mapper扫描路径
 */
@SpringBootApplication
@MapperScan("com.animeblog.mapper")
public class AnimeBlogApplication {
    
    /**
     * 应用启动入口
     * 支持--genhash参数用于生成BCrypt密码哈希
     * @param args 启动参数
     */
    public static void main(String[] args) {
        // 如果传入 --genhash 参数，只生成 hash 然后退出
        if (args.length > 0 && args[0].equals("--genhash")) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            System.out.println("HASH:" + encoder.encode("admin123"));
            return;
        }
        SpringApplication.run(AnimeBlogApplication.class, args);
    }
}
