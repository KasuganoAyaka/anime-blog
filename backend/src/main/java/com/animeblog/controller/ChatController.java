package com.animeblog.controller;

import com.animeblog.dto.ApiResponse;
import com.animeblog.dto.ChatMessageRequest;
import com.animeblog.dto.ChatRoomReadRequest;
import com.animeblog.entity.User;
import com.animeblog.exception.AuthException;
import com.animeblog.service.ChatService;
import com.animeblog.util.AuthHelper;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天控制器
 * 提供聊天相关功能,包括私聊消息、全局消息、好友管理、黑名单、消息搜索等
 * URL前缀: /api/chat
 * 访问权限: 需要登录
 */
@Validated
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final AuthHelper authHelper;
    private final ChatService chatService;

    // 构造函数注入依赖
    public ChatController(AuthHelper authHelper, ChatService chatService) {
        this.authHelper = authHelper;
        this.chatService = chatService;
    }

    /**
     * 获取聊天初始化数据(包含好友列表、会话列表、未读计数等)
     * URL: GET /api/chat/bootstrap
     * 权限: 登录用户
     * @return 聊天初始化数据
     */
    @GetMapping("/bootstrap")
    public ApiResponse<?> getBootstrap(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.getBootstrap(currentUser));
    }

    /**
     * 获取与指定用户的私聊消息
     * URL: GET /api/chat/private/{targetUserId}/messages
     * 权限: 登录用户
     * 参数: targetUserId-目标用户ID, beforeMessageId-分页游标(获取此ID之前的消息), limit-消息数量限制
     * @return 私聊消息列表
     */
    @GetMapping("/private/{targetUserId}/messages")
    public ApiResponse<?> getPrivateMessages(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long targetUserId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(required = false) Integer limit) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.getPrivateMessages(currentUser, targetUserId, beforeMessageId, limit));
    }

    /**
     * 获取全局聊天消息(公共聊天频道)
     * URL: GET /api/chat/global/messages
     * 权限: 登录用户
     * 参数: beforeMessageId-分页游标, limit-消息数量限制
     * @return 全局消息列表
     */
    @GetMapping("/global/messages")
    public ApiResponse<?> getGlobalMessages(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(required = false) Integer limit) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.getGlobalMessages(beforeMessageId, limit));
    }

    /**
     * 发送好友申请或直接添加好友
     * URL: POST /api/chat/friend-requests/{targetUserId} 或 POST /api/chat/friends/{targetUserId}
     * 权限: 登录用户
     * 参数: targetUserId-目标用户ID
     * @return 好友申请结果
     */
    @PostMapping({"/friend-requests/{targetUserId}", "/friends/{targetUserId}"})
    public ApiResponse<?> requestFriend(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long targetUserId) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.requestFriend(currentUser, targetUserId));
    }

    /**
     * 接受好友申请
     * URL: POST /api/chat/friend-requests/{requestId}/accept
     * 权限: 登录用户(仅好友申请的接收者可操作)
     * 参数: requestId-好友申请ID
     * @return 处理结果
     */
    @PostMapping("/friend-requests/{requestId}/accept")
    public ApiResponse<?> acceptFriendRequest(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long requestId) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.acceptFriendRequest(currentUser, requestId));
    }

    /**
     * 拒绝好友申请
     * URL: POST /api/chat/friend-requests/{requestId}/reject
     * 权限: 登录用户(仅好友申请的接收者可操作)
     * 参数: requestId-好友申请ID
     * @return 处理结果
     */
    @PostMapping("/friend-requests/{requestId}/reject")
    public ApiResponse<?> rejectFriendRequest(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long requestId) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.rejectFriendRequest(currentUser, requestId));
    }

    /**
     * 取消好友申请(撤回自己发送的好友申请)
     * URL: DELETE /api/chat/friend-requests/{requestId}
     * 权限: 登录用户(仅好友申请的发送者可操作)
     * 参数: requestId-好友申请ID
     * @return 取消结果
     */
    @DeleteMapping("/friend-requests/{requestId}")
    public ApiResponse<?> cancelFriendRequest(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long requestId) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        chatService.cancelFriendRequest(currentUser, requestId);
        return ApiResponse.success();
    }

    /**
     * 删除好友
     * URL: DELETE /api/chat/friends/{targetUserId}
     * 权限: 登录用户
     * 参数: targetUserId-好友用户ID
     * @return 删除结果
     */
    @DeleteMapping("/friends/{targetUserId}")
    public ApiResponse<?> removeFriend(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long targetUserId) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.removeFriend(currentUser, targetUserId));
    }

    /**
     * 拉黑用户(将指定用户加入黑名单)
     * URL: POST /api/chat/blocks/{targetUserId}
     * 权限: 登录用户
     * 参数: targetUserId-目标用户ID
     * @return 拉黑结果
     */
    @PostMapping("/blocks/{targetUserId}")
    public ApiResponse<?> blockUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long targetUserId) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.blockUser(currentUser, targetUserId));
    }

    /**
     * 解除拉黑(将指定用户从黑名单移除)
     * URL: DELETE /api/chat/blocks/{targetUserId}
     * 权限: 登录用户
     * 参数: targetUserId-目标用户ID
     * @return 解除结果
     */
    @DeleteMapping("/blocks/{targetUserId}")
    public ApiResponse<?> unblockUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long targetUserId) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.unblockUser(currentUser, targetUserId));
    }

    /**
     * 标记会话已读(将指定聊天室的消息标记为已读)
     * URL: POST /api/chat/read
     * 权限: 登录用户
     * 参数: ChatRoomReadRequest对象(包含roomId等信息)
     * @return 标记结果
     */
    @PostMapping("/read")
    public ApiResponse<?> markRoomRead(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Validated @RequestBody ChatRoomReadRequest request) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        chatService.markRoomRead(currentUser, request);
        return ApiResponse.success();
    }

    /**
     * 搜索聊天消息(在当前会话中搜索关键词)
     * URL: GET /api/chat/search/messages
     * 权限: 登录用户
     * 参数: keyword-搜索关键词, targetUserId-目标用户ID(私聊时指定,不指定则搜索全局消息)
     * @return 匹配的消息列表
     */
    @GetMapping("/search/messages")
    public ApiResponse<?> searchMessages(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String keyword,
            @RequestParam(required = false) Long targetUserId) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException("未登录");
        }
        return ApiResponse.success(chatService.searchMessages(currentUser, keyword, targetUserId));
    }

    /**
     * 搜索联系人(根据关键词搜索好友)
     * URL: GET /api/chat/search/contacts
     * 权限: 登录用户
     * 参数: keyword-搜索关键词
     * @return 匹配的联系人列表
     */
    @GetMapping("/search/contacts")
    public ApiResponse<?> searchContacts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String keyword) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("鏈櫥褰?");
        }
        try {
            return ApiResponse.success(chatService.searchContacts(currentUser, keyword));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 发送聊天消息(私聊或全局聊天)
     * URL: POST /api/chat/messages
     * 权限: 登录用户
     * 参数: ChatMessageRequest对象(包含roomId, content, messageType等)
     * @return 发送成功的消息数据
     */
    @PostMapping("/messages")
    public ApiResponse<?> sendMessage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Validated @RequestBody ChatMessageRequest request) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录");
        }
        try {
            return ApiResponse.success(chatService.sendMessage(currentUser, request));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
