package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ip_region_cache")
public class IpRegionCache {

    @TableId(value = "ip", type = IdType.INPUT)
    private String ip;

    private String region;

    private LocalDateTime expiresAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
