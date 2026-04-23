package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系消息实体类
 * 对应数据库contact_message表,存储用户通过联系表单发送的消息
 */
@Data
@TableName("contact_message")
public class ContactMessage {
    
    /** 消息ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 发送者姓名 */
    private String name;
    
    /** 发送者邮箱 */
    private String email;
    
    /** 消息主题 */
    private String subject;
    
    /** 消息内容 */
    private String message;
    
    /** 处理状态: 0-未处理, 1-已处理 */
    private Integer status;
    
    /** 回复内容 */
    private String replyContent;
    
    /** 回复时间 */
    private LocalDateTime repliedTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
