package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 邮箱验证码实体类
 * 对应数据库email_code表,存储邮箱验证码信息
 */
@Data
@TableName("email_code")
public class EmailCode {
    
    /** 验证码ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 接收验证码的邮箱地址 */
    private String email;
    
    /** 验证码 */
    private String code;
    
    /** 验证码类型: register-注册, reset-重置密码, login-登录验证 */
    private String type;
    
    /** 过期时间 */
    private LocalDateTime expireTime;
    
    /** 使用状态: 0-未使用, 1-已使用 */
    private Integer used;
    
    /** 创建时间 */
    private LocalDateTime createTime;
}
