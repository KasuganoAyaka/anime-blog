package com.animeblog.dto;

import lombok.Data;

/**
 * 音乐上传响应DTO
 * 用于返回上传音乐文件的元数据信息
 */
@Data
public class MusicUploadResponse {
    /** 音乐标题 */
    private String title;
    /** 艺术家/演唱者 */
    private String artist;
    /** 专辑名称 */
    private String album;
    /** 音乐时长(秒) */
    private Integer duration;
    /** 音乐文件URL */
    private String url;
    /** 封面图片URL */
    private String coverUrl;
    /** 原始文件名 */
    private String originalFilename;
}
