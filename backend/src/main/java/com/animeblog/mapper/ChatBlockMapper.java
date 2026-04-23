package com.animeblog.mapper;

import com.animeblog.entity.ChatBlock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天屏蔽 Mapper 接口
 * 用于操作 chat_block 表,管理用户之间的聊天屏蔽/拉黑关系
 */
@Mapper
public interface ChatBlockMapper extends BaseMapper<ChatBlock> {
}
