package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天好友申请实体类
 * 对应数据库chat_friend_request表,存储好友申请记录
 */
@Data
@TableName("chat_friend_request")
public class ChatFriendRequest {
    
    /** 申请ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 申请者用户ID */
    private Long requesterId;
    
    /** 目标用户ID,被申请添加好友的用户 */
    private Long targetUserId;
    
    /** 申请状态: pending-待处理, accepted-已接受, rejected-已拒绝 */
    private String status;
    
    /** 处理时间,接受或拒绝时记录 */
    private LocalDateTime handledTime;
    
    /** 申请创建时间 */
    private LocalDateTime createTime;
    
    /** 记录更新时间 */
    private LocalDateTime updateTime;
}
