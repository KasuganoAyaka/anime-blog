package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 友情链接实体类
 * 对应数据库friend_link表,存储友情链接信息
 */
@Data
@TableName("friend_link")
public class FriendLink {
    
    /** 友情链接ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 网站名称 */
    private String name;
    
    /** 网站URL */
    private String url;
    
    /** 网站Logo图片URL */
    private String logo;
    
    /** 网站描述 */
    private String description;
    
    /** 联系人邮箱 */
    private String email;
    
    /** 状态: 0-待审核, 1-正常, 2-已拒绝 */
    private Integer status;
    
    /** 排序权重,数值越小越靠前 */
    private Integer sort;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}
