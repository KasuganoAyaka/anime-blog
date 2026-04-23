package com.animeblog.dto;

import lombok.Data;

/**
 * 刷新令牌请求
 * 用于客户端传递 Refresh Token 以获取新的 Access Token
 */
@Data
public class RefreshTokenRequest {
    /** Refresh Token 字符串 */
    private String refreshToken;
}
