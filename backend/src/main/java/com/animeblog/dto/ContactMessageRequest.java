package com.animeblog.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 联系消息请求DTO
 * 用于用户通过联系表单发送消息,包含姓名、邮箱、主题和内容
 */
@Data
public class ContactMessageRequest {
    /** 发信人姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 100, message = "姓名长度不能超过 100")
    private String name;

    /** 发信人邮箱地址 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;

    /** 消息主题 */
    @NotBlank(message = "主题不能为空")
    @Size(max = 200, message = "主题长度不能超过 200")
    private String subject;

    /** 消息正文内容 */
    @NotBlank(message = "内容不能为空")
    @Size(max = 5000, message = "内容长度不能超过 5000")
    private String message;
}
