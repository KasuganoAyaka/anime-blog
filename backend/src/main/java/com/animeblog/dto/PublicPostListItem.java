package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicPostListItem {

    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String excerpt;
    private String category;
    private String tags;
    private String coverImage;
    private Long userId;
    private String status;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Integer isFeatured;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
