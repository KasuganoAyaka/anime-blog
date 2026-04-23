package com.animeblog.config;

import com.animeblog.entity.User;
import com.animeblog.service.ChatPushService;
import com.animeblog.util.AuthHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 聊天WebSocket处理器
 * 处理WebSocket连接、断开和消息推送
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    /** WebSocket会话中存储用户ID的属性名 */
    private static final String USER_ID_ATTR = "chatUserId";

    /** 认证辅助工具 */
    private final AuthHelper authHelper;
    /** 聊天推送服务 */
    private final ChatPushService chatPushService;

    /**
     * 构造函数,注入依赖
     * @param authHelper 认证辅助工具
     * @param chatPushService 聊天推送服务
     */
    public ChatWebSocketHandler(AuthHelper authHelper, ChatPushService chatPushService) {
        this.authHelper = authHelper;
        this.chatPushService = chatPushService;
    }

    /**
     * WebSocket连接建立后回调
     * 验证用户身份并注册到推送服务
     * @param session WebSocket会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从连接URL中提取token
        String token = extractToken(session);
        // 验证token并获取用户信息
        User user = authHelper.getCurrentUser(token);
        if (user == null) {
            // 认证失败,关闭连接
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }

        // 将用户ID存入会话属性
        session.getAttributes().put(USER_ID_ATTR, user.getId());
        // 注册到推送服务
        chatPushService.register(user.getId(), session);
        // 向客户端发送连接成功消息
        session.sendMessage(new TextMessage("{\"type\":\"chat.connected\"}"));
    }

    /**
     * WebSocket连接关闭后回调
     * 从推送服务中注销用户
     * @param session WebSocket会话
     * @param status 关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get(USER_ID_ATTR);
        if (userId instanceof Long) {
            chatPushService.unregister((Long) userId, session);
        }
    }

    /**
     * 从WebSocket连接URL中提取认证token
     * 解析查询参数中的token值
     * @param session WebSocket会话
     * @return token字符串,如果不存在则返回null
     */
    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        String query = (uri == null) ? null : uri.getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }

        // 解析URL查询参数
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params.get("token");
    }
}
