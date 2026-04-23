package com.animeblog.mapper;

import com.animeblog.entity.ChatFriendRequest;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天好友请求 Mapper 接口
 * 用于操作 chat_friend_request 表,管理好友申请记录
 */
@Mapper
public interface ChatFriendRequestMapper extends BaseMapper<ChatFriendRequest> {
}
