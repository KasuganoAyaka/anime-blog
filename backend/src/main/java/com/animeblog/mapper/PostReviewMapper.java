package com.animeblog.mapper;

import com.animeblog.entity.PostReview;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子评论 Mapper 接口
 * 用于操作 post_review 表,管理帖子评论/回复的增删改查操作
 */
@Mapper
public interface PostReviewMapper extends BaseMapper<PostReview> {}
