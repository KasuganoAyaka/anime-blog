package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体类
 * <p>
 * 用途：
 * - 存储用户的 Refresh Token，用于在 Access Token 过期后获取新的 Access Token
 * - 实现长期登录，无需用户频繁重新登录
 * - 支持 Token 撤销（退出登录、设备管理等）
 * </p>
 * 
 * <p>
 * 安全特性：
 * - 每个 Refresh Token 有唯一的 JTI（JWT ID）
 * - 存储设备信息，方便用户管理
 * - 支持过期时间和自动清理
 * - 可被主动撤销（退出登录、密码修改等场景）
 * </p>
 */
@Data
@TableName("refresh_token")
public class RefreshToken {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户ID */
    private Long userId;

    /** Refresh Token 字符串（JWT） */
    private String token;

    /** JWT ID（唯一标识符，用于快速查询和撤销） */
    private String jti;

    /** 设备信息（浏览器、操作系统等） */
    private String deviceInfo;

    /** IP 地址（创建时的客户端 IP） */
    private String ipAddress;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 是否已撤销（0-有效，1-已撤销） */
    private Integer revoked;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * 检查 Refresh Token 是否已过期
     * 
     * @return true 如果已过期，false 否则
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 检查 Refresh Token 是否已被撤销
     * 
     * @return true 如果已撤销，false 否则
     */
    public boolean isRevokedFlag() {
        return revoked != null && revoked == 1;
    }

    /**
     * 检查 Refresh Token 是否有效（未过期且未撤销）
     * 
     * @return true 如果有效，false 否则
     */
    public boolean isValid() {
        return !isExpired() && !isRevokedFlag();
    }
}
