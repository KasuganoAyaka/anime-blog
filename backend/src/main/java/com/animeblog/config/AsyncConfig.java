package com.animeblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置类
 * 启用Spring异步功能并配置线程池
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 邮件任务专用线程池
     * 用于异步发送邮件,避免阻塞主线程
     * @return 邮件任务执行器
     */
    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数:保持运行的最小线程数
        executor.setCorePoolSize(2);
        // 最大线程数:允许创建的最大线程数
        executor.setMaxPoolSize(4);
        // 队列容量:等待执行的任务数
        executor.setQueueCapacity(100);
        // 线程名称前缀:便于调试和日志追踪
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "moderationTaskExecutor")
    public Executor moderationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("moderation-");
        executor.initialize();
        return executor;
    }
}
