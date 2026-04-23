package com.animeblog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

/**
 * 验证邮件服务
 * 负责发送各类验证邮件,包括:
 * - 验证码邮件(注册/换邮箱/重置密码)
 * - 联系留言回复通知邮件
 * 使用HTML模板构建美观的邮件内容
 */
@Service
public class VerificationEmailService {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-name:Ayaka Blog}")
    private String fromName;

    @Value("${app.mail.from-address:${spring.mail.username:}}")
    private String fromAddress;

    public VerificationEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 检查邮件服务是否已配置
     * 如果发件人地址为空,则认为邮件服务未配置完成
     *
     * @throws IllegalStateException 邮件服务未配置时抛出
     */
    public void ensureConfigured() {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("邮件服务尚未配置完成");
        }
    }

    /**
     * 发送验证码邮件
     *
     * @param email 目标邮箱
     * @param code  验证码
     * @param type  验证码类型(register/change_email/forget)
     */
    public void sendVerificationCode(String email, String code, String type) {
        String actionLabel = getActionLabel(type);
        String subject = "Ayaka Blog " + actionLabel + "验证码";
        String html = buildVerificationHtml(actionLabel, code);
        sendHtmlEmail(email, subject, html, "验证码发送失败，请稍后重试");
    }

    /**
     * 发送联系留言回复通知邮件
     *
     * @param email          留言者邮箱
     * @param recipientName  留言者姓名
     * @param originalSubject 原留言主题
     * @param replyContent   回复内容
     */
    public void sendContactReply(String email, String recipientName, String originalSubject, String replyContent) {
        String safeName = (recipientName == null || recipientName.isBlank()) ? "朋友" : recipientName.trim();
        String safeSubject = (originalSubject == null || originalSubject.isBlank()) ? "站内留言" : originalSubject.trim();
        String subject = "Re: " + safeSubject + " | Ayaka Blog";
        String html = buildContactReplyHtml(safeName, safeSubject, replyContent);
        sendHtmlEmail(email, subject, html, "回复邮件发送失败，请稍后重试");
    }

    /**
     * 发送HTML格式邮件
     *
     * @param email          收件人
     * @param subject        邮件主题
     * @param html           HTML内容
     * @param fallbackMessage 发送失败时的错误提示
     */
    private void sendHtmlEmail(String email, String subject, String html, String fallbackMessage) {
        ensureConfigured();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()).toString());
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(html, true); // true表示HTML格式
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {} with subject {}", email, subject, e);
            throw new IllegalStateException(fallbackMessage);
        }
    }

    /**
     * 根据验证码类型获取对应的操作标签
     */
    private String getActionLabel(String type) {
        switch (type) {
            case "register":
                return "注册";
            case "change_email":
                return "更换邮箱";
            case "forget":
                return "重置密码";
            default:
                return "身份验证";
        }
    }

    /**
     * 构建验证码邮件的HTML内容
     */
    private String buildVerificationHtml(String actionLabel, String code) {
        return "<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.8;color:#1f2937;\">"
                + "<h2 style=\"margin:0 0 16px;color:#111827;\">Ayaka Blog 邮件验证</h2>"
                + "<p>你正在进行 <strong>" + actionLabel + "</strong> 操作。</p>"
                + "<p>本次验证码为：</p>"
                + "<div style=\"display:inline-block;padding:12px 20px;margin:8px 0 16px;font-size:28px;"
                + "font-weight:700;letter-spacing:6px;background:#f3f4f6;border-radius:12px;color:#111827;\">"
                + code
                + "</div>"
                + "<p>验证码 5 分钟内有效，请尽快完成操作。</p>"
                + "<p>如果这不是你本人发起的请求，请忽略这封邮件。</p>"
                + "</div>";
    }

    /**
     * 构建联系留言回复邮件的HTML内容
     * 对回复内容进行HTML转义,防止XSS攻击
     */
    private String buildContactReplyHtml(String recipientName, String originalSubject, String replyContent) {
        // 对回复内容进行HTML转义
        String normalizedReply = replyContent == null
                ? ""
                : replyContent.trim().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\r\n", "\n").replace("\n", "<br/>");

        return "<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.8;color:#1f2937;\">"
                + "<h2 style=\"margin:0 0 16px;color:#111827;\">Ayaka Blog 留言回复</h2>"
                + "<p>" + recipientName + "，你好：</p>"
                + "<p>你提交的留言主题为：<strong>" + escapeHtml(originalSubject) + "</strong></p>"
                + "<div style=\"margin:16px 0;padding:16px 18px;background:#f8fafc;border:1px solid #e5e7eb;border-radius:14px;\">"
                + normalizedReply
                + "</div>"
                + "<p style=\"margin-top:20px;\">如果你还有其他问题，欢迎继续联系我们。</p>"
                + "<p style=\"color:#6b7280;font-size:13px;\">此邮件由 Ayaka Blog 自动发送，请勿直接回复此邮件。</p>"
                + "</div>";
    }

    /**
     * HTML转义,防止XSS攻击
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
