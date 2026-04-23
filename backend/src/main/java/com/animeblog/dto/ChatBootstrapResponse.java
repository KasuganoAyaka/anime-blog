package com.animeblog.dto;

import lombok.Data;

import java.util.List;

/**
 * 聊天初始化引导响应DTO
 * 返回聊天系统初始化所需的全部数据,包括联系人、对话列表、消息等
 */
@Data
public class ChatBootstrapResponse {
    
    /** 公共聊天室房间标识 */
    private String globalRoomKey;
    
    /** 公共聊天室名称 */
    private String globalRoomName;
    
    /** 公共聊天室未读消息数 */
    private Integer globalUnreadCount;
    
    /** 联系人列表 */
    private List<ChatContactView> contacts;
    
    /** 对话列表 */
    private List<ChatConversationView> conversations;
    
    /** 收到的好友申请列表 */
    private List<ChatFriendRequestView> incomingRequests;
    
    /** 发出的好友申请列表 */
    private List<ChatFriendRequestView> outgoingRequests;
    
    /** 公共聊天室消息列表 */
    private List<ChatMessageView> globalMessages;
}
