package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息视图DTO
 * 用于展示聊天室中的单条消息,包含发送者信息和消息内容
 */
@Data
public class ChatMessageView {
    /** 消息ID */
    private Long id;
    /** 房间类型(private/group) */
    private String roomType;
    /** 房间标识键 */
    private String roomKey;
    /** 发送者用户ID */
    private Long senderId;
    /** 发送者用户名 */
    private String senderName;
    /** 发送者头像URL */
    private String senderAvatar;
    /** 发送者角色(admin/user) */
    private String senderRole;
    /** 接收者用户ID */
    private Long receiverId;
    /** 消息内容 */
    private String content;
    /** 消息创建时间 */
    private LocalDateTime createTime;
}
