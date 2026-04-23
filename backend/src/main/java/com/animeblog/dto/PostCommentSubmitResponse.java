package com.animeblog.dto;

import lombok.Data;

@Data
public class PostCommentSubmitResponse {

    private String status;

    private Boolean reviewRequired;

    private String message;

    private PostCommentView comment;
}
