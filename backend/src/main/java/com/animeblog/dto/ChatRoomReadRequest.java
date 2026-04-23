package com.animeblog.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天房间已读请求DTO
 * 用于标记某个聊天房间的消息为已读状态
 */
@Data
public class ChatRoomReadRequest {
    /** 房间类型(private/group) */
    @NotBlank(message = "房间类型不能为空")
    private String roomType;
    /** 目标用户ID(私有聊天时需要) */
    private Long targetUserId;
}
