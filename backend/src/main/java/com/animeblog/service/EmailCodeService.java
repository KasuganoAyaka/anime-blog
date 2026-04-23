package com.animeblog.service;

import cn.hutool.core.util.RandomUtil;
import com.animeblog.entity.EmailCode;
import com.animeblog.mapper.EmailCodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 邮箱验证码服务
 * 负责生成验证码、保存到数据库并异步发送邮件
 * 验证码有效期为5分钟,使用前会先使该邮箱同类型的活跃验证码失效
 */
@Service
public class EmailCodeService {

    private final EmailCodeMapper emailCodeMapper;
    private final VerificationEmailService verificationEmailService;
    private final AsyncEmailCodeService asyncEmailCodeService;

    public EmailCodeService(
            EmailCodeMapper emailCodeMapper,
            VerificationEmailService verificationEmailService,
            AsyncEmailCodeService asyncEmailCodeService
    ) {
        this.emailCodeMapper = emailCodeMapper;
        this.verificationEmailService = verificationEmailService;
        this.asyncEmailCodeService = asyncEmailCodeService;
    }

    /**
     * 创建并发送验证码
     * 1. 检查邮件服务配置
     * 2. 使该邮箱同类型的活跃验证码失效(防止重复发送)
     * 3. 生成6位随机数字验证码,有效期5分钟
     * 4. 异步发送验证码邮件
     *
     * @param email 目标邮箱地址
     * @param type  验证码类型(register注册/change_email换邮箱/forget忘记密码)
     */
    @Transactional(rollbackFor = Exception.class)
    public void createAndSendCode(String email, String type) {
        verificationEmailService.ensureConfigured();
        // 使同类型的活跃验证码失效
        emailCodeMapper.invalidateActiveCodes(email, type);

        // 创建新的验证码
        EmailCode emailCode = new EmailCode();
        emailCode.setEmail(email);
        emailCode.setCode(RandomUtil.randomNumbers(6)); // 生成6位随机数字
        emailCode.setType(type);
        emailCode.setExpireTime(LocalDateTime.now().plusMinutes(5)); // 5分钟有效期
        emailCode.setUsed(0); // 0表示未使用
        emailCodeMapper.insert(emailCode);

        // 异步发送邮件,避免阻塞
        asyncEmailCodeService.sendVerificationCodeAsync(
                emailCode.getId(),
                emailCode.getEmail(),
                emailCode.getCode(),
                type
        );
    }
}
