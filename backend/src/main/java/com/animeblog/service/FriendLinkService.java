package com.animeblog.service;

import com.animeblog.entity.FriendLink;
import com.animeblog.mapper.FriendLinkMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 友情链接服务
 * 处理友情链接的申请、审核和展示功能
 */
@Service
public class FriendLinkService {
    @Autowired
    private FriendLinkMapper friendLinkMapper;

    /**
     * 获取已审核通过的友情链接列表
     * 只返回状态为已审核(1)的链接,按排序字段升序排列
     *
     * @return 已审核的友情链接列表
     */
    public List<FriendLink> getApprovedLinks() {
        QueryWrapper<FriendLink> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("sort");
        return friendLinkMapper.selectList(wrapper);
    }

    /**
     * 提交友情链接申请
     * 检查链接是否已存在,防止重复申请
     * 新提交的链接状态为待审核(0)
     *
     * @param name        网站名称
     * @param url         网站链接
     * @param description 网站描述
     * @param email       联系邮箱
     * @return 创建的友链实体
     * @throws IllegalArgumentException 当链接已存在时抛出
     */
    public FriendLink submitLinkApplication(String name, String url, String description, String email) {
        // 检查链接是否已存在
        QueryWrapper<FriendLink> wrapper = new QueryWrapper<>();
        wrapper.eq("url", url.trim()).last("LIMIT 1");
        FriendLink existing = friendLinkMapper.selectOne(wrapper);
        if (existing != null) {
            throw new IllegalArgumentException("该友链已存在或正在审核中");
        }

        FriendLink link = new FriendLink();
        link.setName(name.trim());
        link.setUrl(url.trim());
        link.setDescription(description.trim());
        link.setEmail(email.trim());
        link.setStatus(0); // 0表示待审核
        link.setSort(0);
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        friendLinkMapper.insert(link);
        return link;
    }
}
