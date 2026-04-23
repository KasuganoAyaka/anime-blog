package com.animeblog.mapper;

import com.animeblog.entity.RefreshToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 刷新令牌 Mapper 接口
 * <p>
 * 提供 Refresh Token 的数据库操作
 * </p>
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

    /**
     * 根据 JTI 查询 Refresh Token
     * 
     * @param jti JWT ID
     * @return Refresh Token 实体，不存在则返回 null
     */
    default RefreshToken selectByJti(String jti) {
        return selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<RefreshToken>lambdaQuery()
                .eq(RefreshToken::getJti, jti));
    }

    /**
     * 根据 Token 字符串查询
     * 
     * @param token Refresh Token 字符串
     * @return Refresh Token 实体，不存在则返回 null
     */
    default RefreshToken selectByToken(String token) {
        return selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<RefreshToken>lambdaQuery()
                .eq(RefreshToken::getToken, token));
    }

    /**
     * 根据用户 ID 查询所有有效的 Refresh Token
     * 
     * @param userId 用户 ID
     * @return Refresh Token 列表
     */
    default java.util.List<RefreshToken> selectValidByUserId(Long userId) {
        return selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<RefreshToken>lambdaQuery()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getRevoked, 0)
                .gt(RefreshToken::getExpireTime, java.time.LocalDateTime.now())
                .orderByDesc(RefreshToken::getCreateTime));
    }
}
