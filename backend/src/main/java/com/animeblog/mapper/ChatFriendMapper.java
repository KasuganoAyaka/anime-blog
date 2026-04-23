package com.animeblog.mapper;

import com.animeblog.entity.ChatFriend;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天好友 Mapper 接口
 * 用于操作 chat_friend 表,管理用户的好友关系
 */
@Mapper
public interface ChatFriendMapper extends BaseMapper<ChatFriend> {
}
