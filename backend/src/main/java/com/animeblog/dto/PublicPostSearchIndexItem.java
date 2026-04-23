package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicPostSearchIndexItem {

    private Long id;
    private String title;
    private String category;
    private String summary;
    private String excerpt;
    private String content;
    private LocalDateTime createTime;
}
