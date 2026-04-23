package com.animeblog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket消息推送服务
 * 负责管理用户的WebSocket会话,并向用户推送聊天消息、同步事件等
 * 使用线程安全的数据结构支持并发访问
 */
@Component
public class ChatPushService {
    private final ObjectMapper objectMapper;
    // 存储用户ID与WebSocket会话集合的映射关系
    private final Map<Long, CopyOnWriteArraySet<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public ChatPushService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 注册用户的WebSocket会话
     * 将指定会话添加到对应用户的会话集合中
     *
     * @param userId  用户ID
     * @param session WebSocket会话
     */
    public void register(Long userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    /**
     * 注销用户的WebSocket会话
     * 从会话集合中移除指定会话,如果用户没有任何活跃会话则清理该用户记录
     *
     * @param userId  用户ID
     * @param session WebSocket会话
     */
    public void unregister(Long userId, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        // 如果用户没有活跃会话了,清理该用户记录
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    /**
     * 向指定用户的所有WebSocket会话推送消息
     *
     * @param userId  目标用户ID
     * @param payload 消息内容(将被序列化为JSON)
     */
    public void sendToUser(Long userId, Object payload) {
        CopyOnWriteArraySet<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        broadcastToSessions(sessions, payload);
    }

    /**
     * 向多个用户推送消息
     * 自动去重用户ID列表,逐个推送
     *
     * @param userIds 目标用户ID集合
     * @param payload 消息内容
     */
    public void sendToUsers(Collection<Long> userIds, Object payload) {
        userIds.stream().distinct().forEach(userId -> sendToUser(userId, payload));
    }

    /**
     * 向所有在线用户广播消息
     * 遍历所有用户的WebSocket会话进行推送
     *
     * @param payload 消息内容
     */
    public void broadcast(Object payload) {
        sessionsByUser.values().forEach(sessions -> broadcastToSessions(sessions, payload));
    }

    /**
     * 向一组WebSocket会话广播消息
     * 将payload序列化为JSON后,同步地发送到每个活跃的会话
     *
     * @param sessions WebSocket会话集合
     * @param payload  消息内容
     */
    private void broadcastToSessions(Collection<WebSocketSession> sessions, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ignored) {
            // JSON序列化失败,静默跳过
            return;
        }

        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                // 同步发送,避免并发写操作导致的问题
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException ignored) {
                // 发送失败,静默跳过
            }
        }
    }
}
