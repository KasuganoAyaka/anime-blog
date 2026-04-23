package com.animeblog.service;

import com.animeblog.entity.Music;
import com.animeblog.mapper.MusicMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 音乐服务
 * 提供音乐列表的查询以及上/下一首音乐的切换功能
 */
@Service
public class MusicService {
    @Autowired
    private MusicMapper musicMapper;

    /**
     * 获取所有已启用的音乐列表
     * 按排序字段升序排列
     *
     * @return 已启用的音乐列表
     */
    public List<Music> getAllMusic() {
        QueryWrapper<Music> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort");
        return musicMapper.selectList(wrapper);
    }

    /**
     * 获取下一首音乐
     * 根据当前音乐的排序值,获取排序更大的下一首音乐
     * 如果已经是最后一首,则循环返回第一首
     *
     * @param currentId 当前音乐ID
     * @return 下一首音乐实体,如果当前音乐不存在则返回null
     */
    public Music getNextMusic(Long currentId) {
        Music current = musicMapper.selectById(currentId);
        if (current == null) {
            return null;
        }
        // 查找排序大于当前音乐的下一首
        QueryWrapper<Music> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).gt("sort", current.getSort() != null ? current.getSort() : 0)
               .orderByAsc("sort").last("LIMIT 1");
        Music next = musicMapper.selectOne(wrapper);
        if (next == null) {
            // 已经是最后一首,返回第一首
            wrapper = new QueryWrapper<>();
            wrapper.eq("status", 1).orderByAsc("sort").last("LIMIT 1");
            next = musicMapper.selectOne(wrapper);
        }
        return next;
    }

    /**
     * 获取上一首音乐
     * 根据当前音乐的排序值,获取排序更小的上一首音乐
     * 如果已经是第一首,则循环返回最后一首
     *
     * @param currentId 当前音乐ID
     * @return 上一首音乐实体,如果当前音乐不存在则返回null
     */
    public Music getPrevMusic(Long currentId) {
        Music current = musicMapper.selectById(currentId);
        if (current == null) {
            return null;
        }
        // 查找排序小于当前音乐的上一首
        QueryWrapper<Music> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).lt("sort", current.getSort() != null ? current.getSort() : Integer.MAX_VALUE)
               .orderByDesc("sort").last("LIMIT 1");
        Music prev = musicMapper.selectOne(wrapper);
        if (prev == null) {
            // 已经是第一首,返回最后一首
            wrapper = new QueryWrapper<>();
            wrapper.eq("status", 1).orderByDesc("sort").last("LIMIT 1");
            prev = musicMapper.selectOne(wrapper);
        }
        return prev;
    }
}
