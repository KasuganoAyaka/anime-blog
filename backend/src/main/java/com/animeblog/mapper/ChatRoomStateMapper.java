package com.animeblog.mapper;

import com.animeblog.entity.ChatRoomState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天房间状态 Mapper 接口
 * 用于操作 chat_room_state 表,管理聊天房间的状态信息
 */
@Mapper
public interface ChatRoomStateMapper extends BaseMapper<ChatRoomState> {
}
