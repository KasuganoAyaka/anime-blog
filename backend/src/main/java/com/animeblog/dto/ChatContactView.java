package com.animeblog.dto;

import lombok.Data;

/**
 * 聊天联系人视图DTO
 * 用于显示聊天界面的联系人列表项
 */
@Data
public class ChatContactView {
    
    /** 用户ID */
    private Long id;
    
    /** 用户名 */
    private String username;
    
    /** 昵称 */
    private String nickname;
    
    /** 头像URL */
    private String avatar;
    
    /** 用户角色 */
    private String role;
    
    /** 是否为好友 */
    private Boolean friend;
    
    /** 是否被我拉黑 */
    private Boolean blockedByMe;
    
    /** 是否被我拉黑(对方拉黑我) */
    private Boolean blockedByOther;
    
    /** 是否可以直接聊天(无需好友) */
    private Boolean canChatDirectly;
    
    /** 是否可以添加为好友 */
    private Boolean canAddFriend;
    
    /** 是否可以删除好友关系 */
    private Boolean canRemoveFriend;
    
    /** 是否可以拉黑 */
    private Boolean canBlock;
    
    /** 是否可以取消拉黑 */
    private Boolean canUnblock;
    
    /** 是否有待处理的 incoming 好友申请 */
    private Boolean incomingRequestPending;
    
    /** 是否有待处理的 outgoing 好友申请 */
    private Boolean outgoingRequestPending;
}
