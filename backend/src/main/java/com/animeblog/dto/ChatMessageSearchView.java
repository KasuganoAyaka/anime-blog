package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息搜索视图DTO
 * 用于展示搜索结果中的聊天消息,包含对方用户信息及消息内容
 */
@Data
public class ChatMessageSearchView {
    /** 消息ID */
    private Long id;
    /** 对方用户ID */
    private Long peerUserId;
    /** 对方用户名 */
    private String peerName;
    /** 对方用户头像URL */
    private String peerAvatar;
    /** 消息内容 */
    private String content;
    /** 发送者用户ID */
    private Long senderId;
    /** 发送者用户名 */
    private String senderName;
    /** 发送者角色(admin/user) */
    private String senderRole;
    /** 消息创建时间 */
    private LocalDateTime createTime;
}
