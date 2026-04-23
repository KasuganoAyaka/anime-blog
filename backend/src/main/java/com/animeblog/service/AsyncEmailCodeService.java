package com.animeblog.service;

import com.animeblog.entity.EmailCode;
import com.animeblog.mapper.EmailCodeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步邮件验证码服务
 * 负责异步发送邮箱验证码,并在发送失败时标记验证码为已使用状态
 */
@Service
public class AsyncEmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(AsyncEmailCodeService.class);

    private final VerificationEmailService verificationEmailService;
    private final EmailCodeMapper emailCodeMapper;

    public AsyncEmailCodeService(
            VerificationEmailService verificationEmailService,
            EmailCodeMapper emailCodeMapper
    ) {
        this.verificationEmailService = verificationEmailService;
        this.emailCodeMapper = emailCodeMapper;
    }

    /**
     * 异步发送验证码邮件
     * 使用@Async注解异步执行,避免阻塞主线程
     * 发送失败时会将对应的验证码标记为已使用,防止重复使用
     *
     * @param emailCodeId 验证码记录ID
     * @param email       接收邮箱
     * @param code        验证码
     * @param type        验证码类型(register/change_email/forget等)
     */
    @Async("mailTaskExecutor")
    public void sendVerificationCodeAsync(Long emailCodeId, String email, String code, String type) {
        try {
            verificationEmailService.sendVerificationCode(email, code, type);
        } catch (Exception e) {
            // 发送失败时标记验证码为已使用,防止被重复使用
            markCodeFailed(emailCodeId);
            log.warn("Email code {} marked as used after async delivery failure", emailCodeId);
        }
    }

    /**
     * 将验证码标记为已使用/失败状态
     *
     * @param emailCodeId 验证码记录ID
     */
    private void markCodeFailed(Long emailCodeId) {
        if (emailCodeId == null) {
            return;
        }

        EmailCode emailCode = new EmailCode();
        emailCode.setId(emailCodeId);
        emailCode.setUsed(1);
        emailCodeMapper.updateById(emailCode);
    }
}
