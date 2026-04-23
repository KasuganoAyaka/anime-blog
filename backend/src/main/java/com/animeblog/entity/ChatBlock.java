package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天黑名单实体类
 * 对应数据库chat_block表,存储用户拉黑关系
 */
@Data
@TableName("chat_block")
public class ChatBlock {
    
    /** 黑名单记录ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 拉黑者用户ID */
    private Long userId;
    
    /** 被拉黑者用户ID */
    private Long blockedUserId;
    
    /** 拉黑时间 */
    private LocalDateTime createTime;
}
