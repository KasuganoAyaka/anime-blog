package com.animeblog.mapper;

import com.animeblog.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper 接口
 * 用于操作 chat_message 表,处理聊天消息的增删改查操作
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
