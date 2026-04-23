package com.animeblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.animeblog.entity.Music;
import org.apache.ibatis.annotations.Mapper;

/**
 * 音乐 Mapper 接口
 * 用于操作 music 表,管理博客中的音乐信息
 */
@Mapper
public interface MusicMapper extends BaseMapper<Music> {}
