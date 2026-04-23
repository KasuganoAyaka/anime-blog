package com.animeblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.animeblog.entity.FriendLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 友情链接 Mapper 接口
 * 用于操作 friend_link 表,管理网站的友情链接信息
 */
@Mapper
public interface FriendLinkMapper extends BaseMapper<FriendLink> {}
