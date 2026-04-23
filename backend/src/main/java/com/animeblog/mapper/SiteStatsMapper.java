package com.animeblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.animeblog.entity.SiteStats;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站点统计 Mapper 接口
 * 用于操作 site_stats 表,管理网站统计数据(如访问量、用户数等)
 */
@Mapper
public interface SiteStatsMapper extends BaseMapper<SiteStats> {}
