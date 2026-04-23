package com.animeblog.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天消息发送请求DTO
 * 用于接收发送聊天消息时提交的数据
 */
@Data
public class ChatMessageRequest {
    
    /** 房间类型: private-私聊, global-公共聊天室 */
    @NotBlank(message = "消息类型不能为空")
    private String roomType;

    /** 目标用户ID,私聊时必填 */
    private Long targetUserId;

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过 2000 字")
    private String content;
}
