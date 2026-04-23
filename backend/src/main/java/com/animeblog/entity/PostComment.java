package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_comment")
public class PostComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long userId;

    private Long parentId;

    private String authorName;

    private String authorEmail;

    private String authorWebsite;

    private String authorAvatar;

    private String content;

    private String images;

    private Long likeCount;

    private Integer anonymous;

    private String status;

    private Long reviewedBy;

    private String reviewNote;

    private LocalDateTime reviewedTime;

    private String clientIp;

    private String userAgent;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
