package com.animeblog.dto;

import lombok.Data;

/**
 * 文章审核决定请求DTO
 * 用于提交文章的审核结果(通过/拒绝)及审核意见
 */
@Data
public class PostReviewDecisionRequest {
    /** 审核意见/备注 */
    private String reviewNote;
}
