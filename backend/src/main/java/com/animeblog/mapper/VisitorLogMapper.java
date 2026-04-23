package com.animeblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.animeblog.entity.VisitorLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 访问日志 Mapper 接口
 * 用于操作 visitor_log 表,记录和管理网站访问日志信息
 */
@Mapper
public interface VisitorLogMapper extends BaseMapper<VisitorLog> {}
