package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment_report")
public class CommentReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long commentId;

    private Long postId;

    private Long reporterId;

    private String reasonCode;

    private String reasonLabel;

    private String otherReason;

    private String description;

    private String status;

    private String resolutionAction;

    private String resolutionNote;

    private Long resolvedBy;

    private LocalDateTime resolvedTime;

    private String commentAuthorName;

    private String commentContent;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
