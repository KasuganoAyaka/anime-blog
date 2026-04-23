package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章审核视图DTO
 * 用于展示文章审核相关的完整信息,包括文章内容、审核状态及审核人员信息
 */
@Data
public class PostReviewView {
    /** 审核记录ID */
    private Long id;
    /** 文章ID */
    private Long postId;
    /** 提交者用户ID */
    private Long userId;
    /** 提交者用户名 */
    private String username;
    /** 提交者昵称 */
    private String nickname;
    /** 审核动作(approve/reject) */
    private String action;
    /** 审核状态(pending/approved/rejected) */
    private String reviewStatus;
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
    /** 审核意见/备注 */
    private String reviewNote;
    /** 审核人用户ID */
    private Long reviewerId;
    /** 审核人用户名 */
    private String reviewerName;
    /** 审核完成时间 */
    private LocalDateTime reviewedTime;
    /** 文章创建时间 */
    private LocalDateTime createTime;
    /** 文章更新时间 */
    private LocalDateTime updateTime;
}
