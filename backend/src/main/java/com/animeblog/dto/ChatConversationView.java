package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天对话视图DTO
 * 用于显示对话列表,包含最后消息和未读数
 */
@Data
public class ChatConversationView {
    
    /** 房间类型 */
    private String roomType;
    
    /** 房间标识 */
    private String roomKey;
    
    /** 对话对方用户ID */
    private Long peerUserId;
    
    /** 对话标题,通常为用户昵称 */
    private String title;
    
    /** 用户名 */
    private String username;
    
    /** 昵称 */
    private String nickname;
    
    /** 头像URL */
    private String avatar;
    
    /** 用户角色 */
    private String role;
    
    /** 是否为好友 */
    private Boolean friend;
    
    /** 是否被拉黑 */
    private Boolean blocked;
    
    /** 是否可以直接聊天 */
    private Boolean canChatDirectly;
    
    /** 未读消息数量 */
    private Integer unreadCount;
    
    /** 最后一条消息预览 */
    private String lastMessagePreview;
    
    /** 最后消息时间 */
    private LocalDateTime lastMessageTime;
    
    /** 是否有消息记录 */
    private Boolean hasMessages;
}
