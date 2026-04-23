package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminPostCommentView {

    private Long id;

    private Long postId;

    private String postTitle;

    private Long parentId;

    private String parentAuthorName;

    private String parentContent;

    private String parentStatus;

    private Boolean parentAnonymous;

    private Long userId;

    private String username;

    private String nickname;

    private String authorName;

    private String authorEmail;

    private String authorWebsite;

    private String authorAvatar;

    private String authorRole;

    private Boolean anonymous;

    private String status;

    private String clientRegion;

    private String clientOs;

    private String clientBrowser;

    private String content;

    private List<String> images;

    private Long likeCount;

    private Long reviewedBy;

    private String reviewerName;

    private String reviewNote;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
