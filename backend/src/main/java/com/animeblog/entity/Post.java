package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文章实体类
 * 对应数据库post表,存储博客文章信息
 */
@Data
@TableName("post")
public class Post {
    
    /** 文章ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 文章标题 */
    private String title;
    
    /** 文章别名,用于URL访问 */
    private String slug;
    
    /** 文章内容,Markdown格式 */
    private String content;
    
    /** 文章摘要,手动填写 */
    private String summary;
    
    /** 文章摘录,自动生成或截取 */
    private String excerpt;
    
    /** 文章分类 */
    private String category;
    
    /** 文章标签,逗号分隔 */
    private String tags;
    
    /** 封面图片URL */
    private String coverImage;
    
    /** 作者用户ID */
    private Long userId;
    
    /** 文章状态: draft-草稿, published-已发布, review-审核中 */
    private String status;
    
    /** 浏览次数 */
    private Long viewCount;
    
    /** 点赞次数 */
    private Long likeCount;
    
    /** 评论次数 */
    private Long commentCount;
    
    /** 是否精选: 0-否, 1-是 */
    private Integer isFeatured;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
