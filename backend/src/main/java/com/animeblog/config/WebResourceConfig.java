package com.animeblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web静态资源配置类
 * 配置上传文件的访问路径映射
 */
@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    /** 上传文件存储目录,默认为./storage/uploads */
    @Value("${app.upload.dir:./storage/uploads}")
    private String uploadDir;

    /**
     * 添加静态资源处理器
     * 将/uploads/**路径映射到实际的文件存储目录
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 获取上传目录的绝对路径
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        // 转换为URI格式
        String location = uploadPath.toUri().toString();
        // 配置路径映射: /uploads/** -> 实际文件存储位置
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
