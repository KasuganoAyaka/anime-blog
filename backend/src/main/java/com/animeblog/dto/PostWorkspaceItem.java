package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章工作区项DTO
 * 用于展示用户文章工作区中的文章列表项,包含文章基本信息和审核状态
 */
@Data
public class PostWorkspaceItem {
    /** 项类型(draft/submitted/returned) */
    private String itemType;
    /** 文章ID */
    private Long postId;
    /** 审核记录ID */
    private Long reviewId;
    /** 文章标题 */
    private String title;
    /** 文章URL别名 */
    private String slug;
    /** 文章正文内容 */
    private String content;
    /** 文章摘要 */
    private String summary;
    /** 文章摘录/预览文本 */
    private String excerpt;
    /** 文章分类 */
    private String category;
    /** 文章标签(逗号分隔) */
    private String tags;
    /** 封面图片URL */
    private String coverImage;
    /** 文章状态(draft/published) */
    private String postStatus;
    /** 审核状态(pending/approved/rejected) */
    private String reviewStatus;
    /** 审核动作(approve/reject) */
    private String reviewAction;
    /** 审核意见/备注 */
    private String reviewNote;
    /** 提交审核时间 */
    private LocalDateTime submitTime;
    /** 审核完成时间 */
    private LocalDateTime reviewedTime;
    /** 文章展示时间 */
    private LocalDateTime displayTime;
}
