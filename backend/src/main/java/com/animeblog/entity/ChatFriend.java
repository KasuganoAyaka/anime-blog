package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天好友实体类
 * 对应数据库chat_friend表,存储用户好友关系
 */
@Data
@TableName("chat_friend")
public class ChatFriend {
    
    /** 好友关系ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户ID */
    private Long userId;
    
    /** 好友用户ID */
    private Long friendId;
    
    /** 添加好友时间 */
    private LocalDateTime createTime;
}
