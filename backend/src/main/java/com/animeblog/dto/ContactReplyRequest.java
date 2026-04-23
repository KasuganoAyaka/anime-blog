package com.animeblog.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 联系回复请求DTO
 * 用于管理员回复联系表单消息
 */
@Data
public class ContactReplyRequest {
    /** 回复内容 */
    @NotBlank(message = "回复内容不能为空")
    private String replyContent;
}
