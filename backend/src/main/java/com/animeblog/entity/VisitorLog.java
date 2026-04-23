package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 访客日志实体类
 * 对应数据库visitor_log表,记录网站访问日志信息
 */
@Data
@TableName("visitor_log")
public class VisitorLog {
    
    /** 日志ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 访问者IP地址 */
    private String ip;
    
    /** 浏览器User-Agent信息 */
    private String ua;
    
    /** 访问的页面路径 */
    private String page;
    
    /** 访问时间 */
    private LocalDateTime createTime;
}
