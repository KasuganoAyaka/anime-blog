package com.animeblog.mapper;

import com.animeblog.entity.ContactMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 联系消息 Mapper 接口
 * 用于操作 contact_message 表,管理用户留言或联系消息
 */
@Mapper
public interface ContactMessageMapper extends BaseMapper<ContactMessage> {}
