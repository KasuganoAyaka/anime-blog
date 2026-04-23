package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostCommentView {

    private Long id;

    private Long postId;

    private Long parentId;

    private String authorName;

    private String authorAvatar;

    private String authorRole;

    private String authorWebsite;

    private Boolean anonymous;

    private String status;

    private String clientRegion;

    private String clientOs;

    private String clientBrowser;

    private String content;

    private List<String> images;

    private Long likeCount;

    private String replyToAuthorName;

    private List<PostCommentView> replies;

    private LocalDateTime createdAt;
}
