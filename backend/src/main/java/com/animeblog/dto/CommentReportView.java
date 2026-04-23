package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentReportView {

    private Long id;

    private Long commentId;

    private Long postId;

    private String postTitle;

    private Long parentId;

    private String parentAuthorName;

    private String commentAuthorName;

    private String commentContent;

    private String commentStatus;

    private Boolean commentDeleted;

    private Long reporterId;

    private String reporterName;

    private String reporterRole;

    private String reasonCode;

    private String reasonLabel;

    private String otherReason;

    private String description;

    private String status;

    private String resolutionAction;

    private String resolutionNote;

    private Long resolvedBy;

    private String resolverName;

    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;
}
