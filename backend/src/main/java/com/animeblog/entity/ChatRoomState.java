package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天房间状态实体类
 * 对应数据库chat_room_state表,存储用户聊天房间的未读状态
 */
@Data
@TableName("chat_room_state")
public class ChatRoomState {
    
    /** 状态ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 房间类型: private-私聊, group-群聊 */
    private String roomType;
    
    /** 房间标识 */
    private String roomKey;
    
    /** 对话对方用户ID,私聊时使用 */
    private Long peerUserId;
    
    /** 未读消息数量 */
    private Integer unreadCount;
    
    /** 最后一条消息预览 */
    private String lastMessagePreview;
    
    /** 最后已读时间 */
    private LocalDateTime lastReadTime;
    
    /** 最后消息时间 */
    private LocalDateTime lastMessageTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
