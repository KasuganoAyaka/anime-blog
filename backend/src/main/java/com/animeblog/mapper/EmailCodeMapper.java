package com.animeblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.animeblog.entity.EmailCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 邮箱验证码 Mapper 接口
 * 用于操作 email_code 表,管理邮箱验证码的存储和验证
 */
@Mapper
public interface EmailCodeMapper extends BaseMapper<EmailCode> {

    /**
     * 查询指定邮箱和类型的最新未使用且未过期的验证码
     *
     * @param email 邮箱地址
     * @param type  验证码类型
     * @return 验证码记录,如果不存在则返回 null
     */
    @Select("SELECT * FROM email_code WHERE email = #{email} AND type = #{type} AND used = 0 AND expire_time > NOW() ORDER BY create_time DESC LIMIT 1")
    EmailCode selectLatestCode(String email, String type);

    /**
     * 使指定邮箱和类型的所有未使用验证码失效
     * 通常用于用户获取新验证码时,使旧的未使用验证码失效
     *
     * @param email 邮箱地址
     * @param type  验证码类型
     * @return 受影响的记录数
     */
    @Update("UPDATE email_code SET used = 1 WHERE email = #{email} AND type = #{type} AND used = 0")
    int invalidateActiveCodes(String email, String type);
}
