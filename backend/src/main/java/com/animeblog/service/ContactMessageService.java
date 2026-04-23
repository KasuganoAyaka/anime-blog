package com.animeblog.service;

import com.animeblog.dto.ContactMessageRequest;
import com.animeblog.entity.ContactMessage;
import com.animeblog.mapper.ContactMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 联系消息服务
 * 处理用户通过网站联系表单提交的留言,支持回复和邮件通知
 */
@Service
public class ContactMessageService {

    private final ContactMessageMapper contactMessageMapper;
    private final VerificationEmailService verificationEmailService;

    public ContactMessageService(
            ContactMessageMapper contactMessageMapper,
            VerificationEmailService verificationEmailService
    ) {
        this.contactMessageMapper = contactMessageMapper;
        this.verificationEmailService = verificationEmailService;
    }

    /**
     * 创建联系留言
     * 将用户提交的联系表单信息保存到数据库
     *
     * @param request 联系消息请求数据
     * @return 创建的联系消息实体
     */
    public ContactMessage createMessage(ContactMessageRequest request) {
        ContactMessage message = new ContactMessage();
        message.setName(request.getName().trim());
        message.setEmail(request.getEmail().trim());
        message.setSubject(request.getSubject().trim());
        message.setMessage(request.getMessage().trim());
        message.setStatus(0); // 0表示未回复
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        contactMessageMapper.insert(message);
        return message;
    }

    /**
     * 获取所有联系留言列表
     * 按创建时间倒序排列
     *
     * @return 联系消息列表
     */
    public List<ContactMessage> listMessages() {
        QueryWrapper<ContactMessage> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return contactMessageMapper.selectList(wrapper);
    }

    /**
     * 根据ID获取联系留言
     *
     * @param id 留言ID
     * @return 联系消息实体,不存在时返回null
     */
    public ContactMessage getById(Long id) {
        return contactMessageMapper.selectById(id);
    }

    /**
     * 回复联系留言并发送邮件通知
     * 更新留言的回复内容、回复时间和状态为已回复(1)
     * 同时向留言发送者发送回复邮件通知
     *
     * @param id         留言ID
     * @param replyContent 回复内容
     * @return 更新后的联系消息实体
     * @throws IllegalArgumentException 当留言不存在或回复内容为空时抛出
     */
    public ContactMessage replyMessage(Long id, String replyContent) {
        ContactMessage existing = contactMessageMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("联系消息不存在");
        }

        String normalizedReply = replyContent == null ? "" : replyContent.trim();
        if (normalizedReply.isBlank()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }

        // 发送邮件回复通知
        verificationEmailService.sendContactReply(
                existing.getEmail(),
                existing.getName(),
                existing.getSubject(),
                normalizedReply
        );

        // 更新留言状态
        LocalDateTime now = LocalDateTime.now();
        existing.setReplyContent(normalizedReply);
        existing.setRepliedTime(now);
        existing.setStatus(1); // 1表示已回复
        existing.setUpdateTime(now);
        contactMessageMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除联系留言
     *
     * @param id 留言ID
     */
    public void deleteMessage(Long id) {
        contactMessageMapper.deleteById(id);
    }
}
