package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 站点统计实体类
 * 对应数据库site_stats表,存储网站各项统计数据
 */
@Data
@TableName("site_stats")
public class SiteStats {
    
    /** 统计ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 统计项键名,如: total_posts, total_users */
    private String statKey;
    
    /** 统计项数值 */
    private Long statValue;
    
    /** 统计项描述 */
    private String description;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
