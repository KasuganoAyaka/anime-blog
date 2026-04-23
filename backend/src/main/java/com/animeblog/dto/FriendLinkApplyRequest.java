package com.animeblog.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 友情链接申请请求DTO
 * 用于用户申请添加友情链接,包含网站信息和联系方式
 */
@Data
public class FriendLinkApplyRequest {
    /** 网站名称 */
    @NotBlank(message = "网站名称不能为空")
    @Size(max = 100, message = "网站名称长度不能超过 100")
    private String siteName;

    /** 网站URL地址 */
    @NotBlank(message = "网站 URL 不能为空")
    @Pattern(regexp = "^https?://.+", message = "网站 URL 格式不正确")
    @Size(max = 500, message = "网站 URL 长度不能超过 500")
    private String siteUrl;

    /** 网站描述/简介 */
    @NotBlank(message = "网站描述不能为空")
    @Size(max = 1000, message = "网站描述长度不能超过 1000")
    private String siteDesc;

    /** 联系邮箱 */
    @NotBlank(message = "联系邮箱不能为空")
    @Email(message = "联系邮箱格式不正确")
    @Size(max = 100, message = "联系邮箱长度不能超过 100")
    private String contactEmail;
}
