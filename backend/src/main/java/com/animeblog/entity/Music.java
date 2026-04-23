package com.animeblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 音乐实体类
 * 对应数据库music表,存储音乐信息
 */
@Data
@TableName("music")
public class Music {
    
    /** 音乐ID,主键,自增 */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 音乐标题 */
    private String title;
    
    /** 艺术家/歌手名称 */
    private String artist;
    
    /** 专辑名称 */
    private String album;
    
    /** 音乐文件URL */
    private String url;
    
    /** 封面图片URL */
    private String coverUrl;
    
    /** 音乐时长(秒) */
    private Integer duration;
    
    /** 歌词内容 */
    private String lyrics;
    
    /** 排序权重,数值越小越靠前 */
    private Integer sort;
    
    /** 状态: 0-禁用, 1-正常 */
    private Integer status;
    
    /** 播放次数 */
    private Long playCount;

    /** 创建时间,插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间,插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
