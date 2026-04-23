package com.animeblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.animeblog.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper 接口
 * 用于操作 user 表,管理用户信息的增删改查操作
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息,如果不存在则返回 null
     */
    @Select("SELECT * FROM user WHERE username = #{username} LIMIT 1")
    User selectByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱地址
     * @return 用户信息,如果不存在则返回 null
     */
    @Select("SELECT * FROM user WHERE email = #{email} LIMIT 1")
    User selectByEmail(String email);
}
