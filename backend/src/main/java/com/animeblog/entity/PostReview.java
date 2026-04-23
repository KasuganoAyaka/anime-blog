package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章审核实体类
 * 对应数据库post_review表,存储文章审核记录和修订历史
 */
@Data
@TableName("post_review")
public class PostReview {
    
    /** 审核记录ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 关联的文章ID */
    private Long postId;
    
    /** 提交审核的用户ID */
    private Long userId;
    
    /** 审核操作: submit-提交, approve-通过, reject-拒绝 */
    private String action;
    
    /** 审核状态: pending-待审核, approved-已通过, rejected-已拒绝 */
    private String reviewStatus;
    
    /** 审核时的文章标题快照 */
    private String title;
    
    /** 审核时的文章别名快照 */
    private String slug;
    
    /** 审核时的文章内容快照 */
    private String content;
    
    /** 审核时的文章摘要快照 */
    private String summary;
    
    /** 审核时的文章摘录快照 */
    private String excerpt;
    
    /** 审核时的文章分类快照 */
    private String category;
    
    /** 审核时的文章标签快照 */
    private String tags;
    
    /** 审核时的封面图片快照 */
    private String coverImage;
    
    /** 审核时的文章状态快照 */
    @TableField("post_status")
    private String postStatus;
    
    /** 审核备注 */
    private String reviewNote;
    
    /** 审核人ID */
    private Long reviewerId;
    
    /** 审核时间 */
    private LocalDateTime reviewedTime;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
