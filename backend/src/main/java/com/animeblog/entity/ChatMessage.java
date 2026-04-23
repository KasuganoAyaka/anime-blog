package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * 对应数据库chat_message表,存储聊天消息记录
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    
    /** 消息ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 房间类型: private-私聊, group-群聊 */
    private String roomType;
    
    /** 房间标识,私聊时为双方用户ID组合,群聊时为群ID */
    private String roomKey;
    
    /** 发送者用户ID */
    private Long senderId;
    
    /** 接收者用户ID,私聊时使用 */
    private Long receiverId;
    
    /** 消息内容 */
    private String content;
    
    /** 消息创建时间 */
    private LocalDateTime createTime;
}
