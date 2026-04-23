package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库user表,存储用户账户信息
 */
@Data
@TableName("user")
public class User {
    
    /** 用户ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 用户名,用于登录 */
    private String username;
    
    /** 密码,加密存储 */
    private String password;
    
    /** 邮箱地址 */
    private String email;
    
    /** 邮箱验证状态: 0-未验证, 1-已验证 */
    @TableField("email_verified")
    private Integer emailVerified;
    
    /** 用户头像URL */
    private String avatar;
    
    /** 昵称,显示名称 */
    private String nickname;
    
    /** 个人简介 */
    private String bio;
    
    /** 用户角色: admin-管理员, user-普通用户 */
    private String role;
    
    /** 用户状态: 0-禁用, 1-正常 */
    private Integer status;
    
    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;
    
    /** 最后登录IP地址 */
    private String lastLoginIp;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
