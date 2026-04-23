package com.animeblog.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 带自动过期清理的缓存
 * <p>
 * 特性：
 * 1. 支持设置条目的过期时间
 * 2. 定时后台线程自动清理过期条目
 * 3. 线程安全（基于 ConcurrentHashMap）
 * 4. 防止内存泄漏和 DoS 攻击
 * </p>
 * 
 * <p>
 * 使用场景：
 * - 验证码缓存（5分钟过期）
 * - 临时 Token 缓存
 * - 任何需要自动清理的短期数据
 * </p>
 * 
 * @param <V> 缓存值类型
 */
public class ExpiringCache<V> {

    private static final Logger log = LoggerFactory.getLogger(ExpiringCache.class);

    /** 底层存储 */
    private final ConcurrentHashMap<String, CacheEntry<V>> cache = new ConcurrentHashMap<>();

    /** 定时清理任务执行器 */
    private final ScheduledExecutorService cleanupExecutor;

    /** 清理间隔（秒） */
    private final long cleanupIntervalSeconds;

    /**
     * 缓存条目，包含值和过期时间
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long expireTime;

        CacheEntry(V value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 创建带自动清理的缓存
     * 
     * @param cleanupIntervalSeconds 清理间隔（秒）
     */
    public ExpiringCache(long cleanupIntervalSeconds) {
        this.cleanupIntervalSeconds = cleanupIntervalSeconds;
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "expiring-cache-cleanup");
            thread.setDaemon(true); // 守护线程，JVM 关闭时自动终止
            return thread;
        });

        // 启动定时清理任务
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredEntries,
                cleanupIntervalSeconds,
                cleanupIntervalSeconds,
                TimeUnit.SECONDS
        );

        log.info("ExpiringCache 已启动，清理间隔: {} 秒", cleanupIntervalSeconds);
    }

    /**
     * 添加缓存条目
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param ttlMillis 存活时间（毫秒）
     */
    public void put(String key, V value, long ttlMillis) {
        long expireTime = System.currentTimeMillis() + ttlMillis;
        cache.put(key, new CacheEntry<>(value, expireTime));
    }

    /**
     * 获取缓存条目
     * 
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期则返回 null
     */
    public V get(String key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            // 惰性删除过期条目
            cache.remove(key);
            return null;
        }

        return entry.value;
    }

    /**
     * 删除缓存条目
     * 
     * @param key 缓存键
     * @return 被删除的值，如果不存在则返回 null
     */
    public V remove(String key) {
        CacheEntry<V> entry = cache.remove(key);
        return entry != null ? entry.value : null;
    }

    /**
     * 检查缓存条目是否存在且未过期
     * 
     * @param key 缓存键
     * @return true 如果存在且未过期，false 否则
     */
    public boolean containsKey(String key) {
        return get(key) != null;
    }

    /**
     * 清理所有过期条目
     */
    private void cleanupExpiredEntries() {
        try {
            int cleanedCount = 0;
            long now = System.currentTimeMillis();

            for (var entry : cache.entrySet()) {
                if (entry.getValue().expireTime < now) {
                    cache.remove(entry.getKey());
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                log.debug("清理了 {} 个过期缓存条目，当前缓存大小: {}", cleanedCount, cache.size());
            }
        } catch (Exception e) {
            log.error("缓存清理任务异常", e);
        }
    }

    /**
     * 获取当前缓存大小（仅用于监控和调试）
     * 
     * @return 缓存条目数量
     */
    public int size() {
        return cache.size();
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.clear();
    }

    /**
     * 关闭缓存，停止清理任务
     * <p>
     * 通常在应用关闭时调用
     * </p>
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("ExpiringCache 已关闭");
    }
}
