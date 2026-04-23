package com.animeblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.animeblog.entity.Post;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子 Mapper 接口
 * 用于操作 post 表,管理博客帖子/文章的增删改查操作
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {}
