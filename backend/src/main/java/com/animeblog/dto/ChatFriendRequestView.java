package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天好友请求视图DTO
 * 用于展示好友申请的相关信息,包括申请人、目标用户及申请状态
 */
@Data
public class ChatFriendRequestView {
    /** 申请记录ID */
    private Long id;
    /** 申请方向(IN/OUT),表示是发出还是收到的申请 */
    private String direction;
    /** 申请状态(pending/accepted/rejected) */
    private String status;
    /** 申请人用户ID */
    private Long requesterId;
    /** 申请人用户名 */
    private String requesterName;
    /** 申请人头像URL */
    private String requesterAvatar;
    /** 目标用户ID */
    private Long targetUserId;
    /** 目标用户名 */
    private String targetUserName;
    /** 目标用户头像URL */
    private String targetUserAvatar;
    /** 申请创建时间 */
    private LocalDateTime createTime;
    /** 申请更新时间 */
    private LocalDateTime updateTime;
}
