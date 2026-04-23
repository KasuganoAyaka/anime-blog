package com.animeblog.service;

import com.animeblog.dto.ChatBootstrapResponse;
import com.animeblog.dto.ChatContactView;
import com.animeblog.dto.ChatConversationView;
import com.animeblog.dto.ChatFriendRequestView;
import com.animeblog.dto.ChatMessageRequest;
import com.animeblog.dto.ChatMessageSearchView;
import com.animeblog.dto.ChatMessageView;
import com.animeblog.dto.ChatRoomReadRequest;
import com.animeblog.entity.ChatBlock;
import com.animeblog.entity.ChatFriend;
import com.animeblog.entity.ChatFriendRequest;
import com.animeblog.entity.ChatMessage;
import com.animeblog.entity.ChatRoomState;
import com.animeblog.entity.User;
import com.animeblog.mapper.ChatBlockMapper;
import com.animeblog.mapper.ChatFriendMapper;
import com.animeblog.mapper.ChatFriendRequestMapper;
import com.animeblog.mapper.ChatMessageMapper;
import com.animeblog.mapper.ChatRoomStateMapper;
import com.animeblog.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 聊天服务类
 * 提供完整的聊天功能,包括:私聊、全局聊天室、好友申请与管理、消息发送与查询、消息搜索、黑名单管理等
 * 支持全局聊天室(3天自动清理)和私聊两种模式
 */
@Service
public class ChatService {
    // 房间类型常量
    public static final String ROOM_TYPE_GLOBAL = "global";
    public static final String ROOM_TYPE_PRIVATE = "private";
    // 全局聊天室标识和名称
    public static final String GLOBAL_ROOM_KEY = "global";
    public static final String GLOBAL_ROOM_NAME = "全站聊天室";

    // 好友申请状态常量
    private static final String REQUEST_STATUS_PENDING = "pending";
    private static final String REQUEST_STATUS_ACCEPTED = "accepted";
    private static final String REQUEST_STATUS_REJECTED = "rejected";
    private static final String REQUEST_STATUS_CANCELLED = "cancelled";

    // 各类查询数量限制
    private static final int MESSAGE_LIMIT = 80;      // 消息加载限制
    private static final int HISTORY_LIMIT = 30;      // 历史消息分页限制
    private static final int SEARCH_LIMIT = 60;       // 搜索限制
    private static final int PREVIEW_LIMIT = 120;     // 消息预览长度限制
    private static final int GLOBAL_MESSAGE_RETENTION_DAYS = 3;  // 全局消息保留天数

    private final ChatFriendMapper chatFriendMapper;
    private final ChatFriendRequestMapper chatFriendRequestMapper;
    private final ChatBlockMapper chatBlockMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatRoomStateMapper chatRoomStateMapper;
    private final UserMapper userMapper;
    private final ChatPushService chatPushService;

    public ChatService(
            ChatFriendMapper chatFriendMapper,
            ChatFriendRequestMapper chatFriendRequestMapper,
            ChatBlockMapper chatBlockMapper,
            ChatMessageMapper chatMessageMapper,
            ChatRoomStateMapper chatRoomStateMapper,
            UserMapper userMapper,
            ChatPushService chatPushService) {
        this.chatFriendMapper = chatFriendMapper;
        this.chatFriendRequestMapper = chatFriendRequestMapper;
        this.chatBlockMapper = chatBlockMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatRoomStateMapper = chatRoomStateMapper;
        this.userMapper = userMapper;
        this.chatPushService = chatPushService;
    }

    /**
     * 获取聊天引导数据
     * 包含:全局房间信息、未读消息数、联系人列表、会话列表、好友申请列表、全局消息等
     * 同时确保当前用户有全局聊天室的状态记录
     *
     * @param currentUser 当前登录用户
     * @return 聊天引导响应数据
     */
    public ChatBootstrapResponse getBootstrap(User currentUser) {
        // 确保用户有全局聊天室的状态记录
        ensureGlobalRoomState(currentUser.getId());

        ChatBootstrapResponse response = new ChatBootstrapResponse();
        response.setGlobalRoomKey(GLOBAL_ROOM_KEY);
        response.setGlobalRoomName(GLOBAL_ROOM_NAME);
        response.setGlobalUnreadCount(getGlobalUnreadCount(currentUser.getId()));
        response.setContacts(buildContacts(currentUser));
        response.setConversations(buildConversations(currentUser));
        response.setIncomingRequests(buildFriendRequests(currentUser, true));
        response.setOutgoingRequests(buildFriendRequests(currentUser, false));
        response.setGlobalMessages(getGlobalMessages(null, MESSAGE_LIMIT));
        return response;
    }

    /**
     * 获取私聊消息列表(默认数量限制)
     *
     * @param currentUser  当前登录用户
     * @param targetUserId 目标用户ID
     * @return 私聊消息视图列表
     */
    public List<ChatMessageView> getPrivateMessages(User currentUser, Long targetUserId) {
        return getPrivateMessages(currentUser, targetUserId, null, MESSAGE_LIMIT);
    }

    /**
     * 获取私聊消息列表(支持分页)
     * 检查目标用户是否存在以及是否有权限查看私聊记录
     *
     * @param currentUser   当前登录用户
     * @param targetUserId  目标用户ID
     * @param beforeMessageId 分页锚点,获取此ID之前的消息
     * @param limit         消息数量限制
     * @return 私聊消息视图列表
     */
    public List<ChatMessageView> getPrivateMessages(User currentUser, Long targetUserId, Long beforeMessageId, Integer limit) {
        User targetUser = getActiveUser(targetUserId);
        if (targetUser == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }
        if (!canOpenPrivateConversation(currentUser, targetUser)) {
            throw new IllegalArgumentException("当前无法查看与该用户的私聊记录");
        }

        String roomKey = buildPrivateRoomKey(currentUser.getId(), targetUserId);
        List<ChatMessage> messages = queryMessages(ROOM_TYPE_PRIVATE, roomKey, beforeMessageId, normalizeHistoryLimit(limit, MESSAGE_LIMIT));
        return toMessageViews(messages);
    }

    /**
     * 获取全局聊天室消息
     * 自动清理过期的全局消息(超过3天的消息)
     *
     * @param beforeMessageId 分页锚点
     * @param limit           消息数量限制
     * @return 全局消息视图列表
     */
    public List<ChatMessageView> getGlobalMessages(Long beforeMessageId, Integer limit) {
        cleanupExpiredGlobalMessages();
        List<ChatMessage> messages = queryMessages(ROOM_TYPE_GLOBAL, GLOBAL_ROOM_KEY, beforeMessageId, normalizeHistoryLimit(limit, MESSAGE_LIMIT));
        return toMessageViews(messages);
    }

    /**
     * 搜索联系人
     * 根据关键词搜索用户的昵称、用户名或邮箱
     * 搜索结果按管理员身份和可聊天状态排序
     *
     * @param currentUser 当前登录用户
     * @param keyword     搜索关键词
     * @return 匹配的联系人视图列表
     */
    public List<ChatContactView> searchContacts(User currentUser, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .ne(User::getId, currentUser.getId())
                        .and(wrapper -> wrapper
                                .like(User::getNickname, normalizedKeyword)
                                .or()
                                .like(User::getUsername, normalizedKeyword)
                                .or()
                                .eq(User::getEmail, normalizedKeyword))
                        .last("LIMIT " + SEARCH_LIMIT)
        );

        return users.stream()
                .map(user -> toContactView(currentUser, user))
                .sorted((left, right) -> {
                    if (isAdminRole(left.getRole()) && !isAdminRole(right.getRole())) return -1;
                    if (!isAdminRole(left.getRole()) && isAdminRole(right.getRole())) return 1;
                    if (Boolean.TRUE.equals(left.getCanChatDirectly()) && !Boolean.TRUE.equals(right.getCanChatDirectly())) return -1;
                    if (!Boolean.TRUE.equals(left.getCanChatDirectly()) && Boolean.TRUE.equals(right.getCanChatDirectly())) return 1;
                    return displayName(left).compareToIgnoreCase(displayName(right));
                })
                .collect(Collectors.toList());
    }

    /**
     * 发送好友申请
     * 如果对方已经向自己发送了 pending 状态的申请,则直接接受好友关系
     * 否则创建新的好友申请记录并推送通知
     *
     * @param currentUser  当前登录用户
     * @param targetUserId 目标用户ID
     * @return 目标用户的联系人视图
     */
    @Transactional
    public ChatContactView requestFriend(User currentUser, Long targetUserId) {
        User targetUser = getActiveUser(targetUserId);
        if (targetUser == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }
        validateRequestablePair(currentUser, targetUser);

        ChatFriendRequest reversePending = findPendingRequest(targetUserId, currentUser.getId());
        if (reversePending != null) {
            acceptFriendRequest(currentUser, reversePending.getId());
            return toContactView(currentUser, targetUser);
        }

        ChatFriendRequest request = new ChatFriendRequest();
        request.setRequesterId(currentUser.getId());
        request.setTargetUserId(targetUserId);
        request.setStatus(REQUEST_STATUS_PENDING);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        chatFriendRequestMapper.insert(request);

        pushSync(currentUser.getId(), targetUserId, "request");
        return toContactView(currentUser, targetUser);
    }

    @Transactional
    public ChatFriendRequestView acceptFriendRequest(User currentUser, Long requestId) {
        ChatFriendRequest request = getPendingIncomingRequest(currentUser.getId(), requestId);
        if (request == null) {
            throw new IllegalArgumentException("好友申请不存在或已处理");
        }
        if (isBlocked(request.getRequesterId(), request.getTargetUserId())
                || isBlocked(request.getTargetUserId(), request.getRequesterId())) {
            throw new IllegalArgumentException("存在拉黑关系，无法通过好友申请");
        }

        saveFriendPair(request.getRequesterId(), request.getTargetUserId());
        saveFriendPair(request.getTargetUserId(), request.getRequesterId());
        updateRequestStatus(request, REQUEST_STATUS_ACCEPTED);

        pushSync(request.getRequesterId(), request.getTargetUserId(), "friend");
        return toFriendRequestView(request, currentUser.getId());
    }

    @Transactional
    public ChatFriendRequestView rejectFriendRequest(User currentUser, Long requestId) {
        ChatFriendRequest request = getPendingIncomingRequest(currentUser.getId(), requestId);
        if (request == null) {
            throw new IllegalArgumentException("好友申请不存在或已处理");
        }
        updateRequestStatus(request, REQUEST_STATUS_REJECTED);
        pushSync(request.getRequesterId(), request.getTargetUserId(), "request");
        return toFriendRequestView(request, currentUser.getId());
    }

    @Transactional
    public void cancelFriendRequest(User currentUser, Long requestId) {
        ChatFriendRequest request = chatFriendRequestMapper.selectById(requestId);
        if (request == null
                || !Objects.equals(request.getRequesterId(), currentUser.getId())
                || !REQUEST_STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalArgumentException("好友申请不存在或无法撤回");
        }
        updateRequestStatus(request, REQUEST_STATUS_CANCELLED);
        pushSync(request.getRequesterId(), request.getTargetUserId(), "request");
    }

    @Transactional
    public ChatContactView removeFriend(User currentUser, Long targetUserId) {
        User targetUser = getActiveUser(targetUserId);
        if (targetUser == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }

        deleteFriendPair(currentUser.getId(), targetUserId);
        deleteFriendPair(targetUserId, currentUser.getId());
        pushSync(currentUser.getId(), targetUserId, "friend");
        return toContactView(currentUser, targetUser);
    }

    @Transactional
    public ChatContactView blockUser(User currentUser, Long targetUserId) {
        User targetUser = getActiveUser(targetUserId);
        if (targetUser == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }
        if (Objects.equals(currentUser.getId(), targetUserId)) {
            throw new IllegalArgumentException("不能拉黑自己");
        }

        saveBlockPair(currentUser.getId(), targetUserId);
        deleteFriendPair(currentUser.getId(), targetUserId);
        deleteFriendPair(targetUserId, currentUser.getId());
        cancelPendingRequestsBetween(currentUser.getId(), targetUserId);
        pushSync(currentUser.getId(), targetUserId, "block");
        return toContactView(currentUser, targetUser);
    }

    @Transactional
    public ChatContactView unblockUser(User currentUser, Long targetUserId) {
        User targetUser = getActiveUser(targetUserId);
        if (targetUser == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }

        chatBlockMapper.delete(
                new LambdaQueryWrapper<ChatBlock>()
                        .eq(ChatBlock::getUserId, currentUser.getId())
                        .eq(ChatBlock::getBlockedUserId, targetUserId)
        );
        pushSync(currentUser.getId(), targetUserId, "block");
        return toContactView(currentUser, targetUser);
    }

    @Transactional
    public void markRoomRead(User currentUser, ChatRoomReadRequest request) {
        String roomType = request.getRoomType() == null ? "" : request.getRoomType().trim();
        LocalDateTime now = LocalDateTime.now();
        if (ROOM_TYPE_GLOBAL.equals(roomType)) {
            ChatRoomState state = getOrCreateRoomState(currentUser.getId(), ROOM_TYPE_GLOBAL, GLOBAL_ROOM_KEY, null);
            state.setUnreadCount(0);
            state.setLastReadTime(now);
            state.setUpdateTime(now);
            saveRoomState(state);
            pushSync(currentUser.getId(), currentUser.getId(), "read");
            return;
        }

        if (!ROOM_TYPE_PRIVATE.equals(roomType)) {
            throw new IllegalArgumentException("不支持的房间类型");
        }

        User targetUser = getActiveUser(request.getTargetUserId());
        if (targetUser == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }
        if (!canOpenPrivateConversation(currentUser, targetUser)) {
            throw new IllegalArgumentException("当前无法查看该会话");
        }

        String roomKey = buildPrivateRoomKey(currentUser.getId(), targetUser.getId());
        ChatRoomState state = getOrCreateRoomState(currentUser.getId(), ROOM_TYPE_PRIVATE, roomKey, targetUser.getId());
        state.setUnreadCount(0);
        state.setLastReadTime(now);
        state.setUpdateTime(now);
        saveRoomState(state);
        pushSync(currentUser.getId(), currentUser.getId(), "read");
    }

    public List<ChatMessageSearchView> searchMessages(User currentUser, String keyword, Long targetUserId) {
        if (!isAdmin(currentUser)) {
            throw new IllegalArgumentException("只有管理员可以搜索消息");
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomType, ROOM_TYPE_PRIVATE)
                .and(q -> q.eq(ChatMessage::getSenderId, currentUser.getId())
                        .or()
                        .eq(ChatMessage::getReceiverId, currentUser.getId()))
                .like(ChatMessage::getContent, normalizedKeyword)
                .orderByDesc(ChatMessage::getCreateTime)
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + SEARCH_LIMIT);

        if (targetUserId != null) {
            wrapper.eq(ChatMessage::getRoomKey, buildPrivateRoomKey(currentUser.getId(), targetUserId));
        }

        return chatMessageMapper.selectList(wrapper).stream()
                .map(message -> toSearchView(currentUser, message))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageView sendMessage(User currentUser, ChatMessageRequest request) {
        String roomType = request.getRoomType() == null ? "" : request.getRoomType().trim();
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        ChatMessage message = new ChatMessage();
        message.setRoomType(roomType);
        message.setSenderId(currentUser.getId());
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());

        if (ROOM_TYPE_GLOBAL.equals(roomType)) {
            cleanupExpiredGlobalMessages();
            message.setRoomKey(GLOBAL_ROOM_KEY);
            chatMessageMapper.insert(message);
            ChatMessageView view = toMessageView(message);
            updateGlobalRoomStates(currentUser.getId(), content, message.getCreateTime());
            pushGlobalMessage(view);
            return view;
        }

        if (!ROOM_TYPE_PRIVATE.equals(roomType)) {
            throw new IllegalArgumentException("不支持的消息类型");
        }

        User targetUser = getActiveUser(request.getTargetUserId());
        if (targetUser == null) {
            throw new IllegalArgumentException("目标用户不存在");
        }
        if (!canOpenPrivateConversation(currentUser, targetUser)) {
            throw new IllegalArgumentException("当前无法向该用户发送私信");
        }

        String roomKey = buildPrivateRoomKey(currentUser.getId(), targetUser.getId());
        message.setRoomKey(roomKey);
        message.setReceiverId(targetUser.getId());
        chatMessageMapper.insert(message);

        updatePrivateRoomStates(currentUser.getId(), targetUser.getId(), roomKey, content, message.getCreateTime());
        ChatMessageView view = toMessageView(message);
        pushPrivateMessage(currentUser.getId(), targetUser.getId(), view);
        return view;
    }

    private List<ChatMessage> queryMessages(String roomType, String roomKey, Long beforeMessageId, int limit) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getRoomType, roomType)
                .eq(ChatMessage::getRoomKey, roomKey);

        if (beforeMessageId != null) {
            wrapper.lt(ChatMessage::getId, beforeMessageId);
        }

        return chatMessageMapper.selectList(
                wrapper.orderByDesc(ChatMessage::getCreateTime)
                        .orderByDesc(ChatMessage::getId)
                        .last("LIMIT " + limit)
        );
    }

    private int normalizeHistoryLimit(Integer limit, int defaultValue) {
        if (limit == null) {
            return defaultValue;
        }
        if (limit < 1) {
            return defaultValue;
        }
        return Math.min(limit, HISTORY_LIMIT);
    }

    private void cleanupExpiredGlobalMessages() {
        LocalDateTime expireBefore = LocalDateTime.now().minusDays(GLOBAL_MESSAGE_RETENTION_DAYS);
        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRoomType, ROOM_TYPE_GLOBAL)
                        .eq(ChatMessage::getRoomKey, GLOBAL_ROOM_KEY)
                        .lt(ChatMessage::getCreateTime, expireBefore)
        );
    }

    private List<ChatContactView> buildContacts(User currentUser) {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .ne(User::getId, currentUser.getId())
        );
        return users.stream()
                .map(user -> toContactView(currentUser, user))
                .sorted(Comparator.comparing((ChatContactView item) -> displayName(item).toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<ChatConversationView> buildConversations(User currentUser) {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getStatus, 1)
                        .ne(User::getId, currentUser.getId())
        );
        Map<Long, ChatRoomState> stateByPeer = chatRoomStateMapper.selectList(
                new LambdaQueryWrapper<ChatRoomState>()
                        .eq(ChatRoomState::getUserId, currentUser.getId())
                        .eq(ChatRoomState::getRoomType, ROOM_TYPE_PRIVATE)
                        .orderByDesc(ChatRoomState::getLastMessageTime)
        ).stream()
                .filter(state -> state.getPeerUserId() != null)
                .collect(Collectors.toMap(ChatRoomState::getPeerUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return users.stream()
                .map(user -> toConversationView(currentUser, user, stateByPeer.get(user.getId())))
                .filter(this::shouldIncludeConversation)
                .sorted(
                        Comparator
                                .comparing((ChatConversationView item) -> item.getUnreadCount() == null ? 0 : item.getUnreadCount()).reversed()
                                .thenComparing((ChatConversationView item) -> item.getLastMessageTime(), Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(item -> item.getTitle().toLowerCase())
                )
                .collect(Collectors.toList());
    }

    private boolean shouldIncludeConversation(ChatConversationView conversation) {
        return Boolean.TRUE.equals(conversation.getHasMessages());
    }

    private List<ChatFriendRequestView> buildFriendRequests(User currentUser, boolean incoming) {
        LambdaQueryWrapper<ChatFriendRequest> wrapper = new LambdaQueryWrapper<ChatFriendRequest>()
                .eq(ChatFriendRequest::getStatus, REQUEST_STATUS_PENDING)
                .orderByDesc(ChatFriendRequest::getUpdateTime)
                .orderByDesc(ChatFriendRequest::getId);

        if (incoming) {
            wrapper.eq(ChatFriendRequest::getTargetUserId, currentUser.getId());
        } else {
            wrapper.eq(ChatFriendRequest::getRequesterId, currentUser.getId());
        }

        return chatFriendRequestMapper.selectList(wrapper).stream()
                .map(request -> toFriendRequestView(request, currentUser.getId()))
                .collect(Collectors.toList());
    }

    private ChatContactView toContactView(User currentUser, User targetUser) {
        boolean currentIsAdmin = isAdmin(currentUser);
        boolean targetIsAdmin = isAdmin(targetUser);
        boolean friend = isFriend(currentUser.getId(), targetUser.getId());
        boolean blockedByMe = isBlocked(currentUser.getId(), targetUser.getId());
        boolean blockedByOther = isBlocked(targetUser.getId(), currentUser.getId());
        boolean incomingPending = findPendingRequest(targetUser.getId(), currentUser.getId()) != null;
        boolean outgoingPending = findPendingRequest(currentUser.getId(), targetUser.getId()) != null;

        ChatContactView view = new ChatContactView();
        view.setId(targetUser.getId());
        view.setUsername(targetUser.getUsername());
        view.setNickname(targetUser.getNickname());
        view.setAvatar(targetUser.getAvatar());
        view.setRole(targetUser.getRole());
        view.setFriend(friend);
        view.setBlockedByMe(blockedByMe);
        view.setBlockedByOther(blockedByOther);
        view.setIncomingRequestPending(incomingPending);
        view.setOutgoingRequestPending(outgoingPending);
        view.setCanChatDirectly(canOpenPrivateConversation(currentUser, targetUser));
        view.setCanAddFriend(!currentIsAdmin
                && !targetIsAdmin
                && !friend
                && !blockedByMe
                && !blockedByOther
                && !incomingPending
                && !outgoingPending);
        view.setCanRemoveFriend(friend);
        view.setCanBlock(!blockedByMe);
        view.setCanUnblock(blockedByMe);
        return view;
    }

    private ChatConversationView toConversationView(User currentUser, User targetUser, ChatRoomState state) {
        ChatContactView contact = toContactView(currentUser, targetUser);
        ChatConversationView view = new ChatConversationView();
        view.setRoomType(ROOM_TYPE_PRIVATE);
        view.setRoomKey(buildPrivateRoomKey(currentUser.getId(), targetUser.getId()));
        view.setPeerUserId(targetUser.getId());
        view.setTitle(displayName(contact));
        view.setUsername(targetUser.getUsername());
        view.setNickname(targetUser.getNickname());
        view.setAvatar(targetUser.getAvatar());
        view.setRole(targetUser.getRole());
        view.setFriend(contact.getFriend());
        view.setBlocked(Boolean.TRUE.equals(contact.getBlockedByMe()) || Boolean.TRUE.equals(contact.getBlockedByOther()));
        view.setCanChatDirectly(contact.getCanChatDirectly());
        view.setUnreadCount(state == null || state.getUnreadCount() == null ? 0 : state.getUnreadCount());
        view.setLastMessagePreview(state == null ? null : state.getLastMessagePreview());
        view.setLastMessageTime(state == null ? null : state.getLastMessageTime());
        view.setHasMessages(state != null);
        return view;
    }

    private ChatFriendRequestView toFriendRequestView(ChatFriendRequest request, Long currentUserId) {
        Map<Long, User> userMap = userMapper.selectByIds(List.of(request.getRequesterId(), request.getTargetUserId())).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
        User requester = userMap.get(request.getRequesterId());
        User targetUser = userMap.get(request.getTargetUserId());

        ChatFriendRequestView view = new ChatFriendRequestView();
        view.setId(request.getId());
        view.setDirection(Objects.equals(request.getRequesterId(), currentUserId) ? "outgoing" : "incoming");
        view.setStatus(request.getStatus());
        view.setRequesterId(request.getRequesterId());
        view.setRequesterName(displayName(requester));
        view.setRequesterAvatar(requester == null ? null : requester.getAvatar());
        view.setTargetUserId(request.getTargetUserId());
        view.setTargetUserName(displayName(targetUser));
        view.setTargetUserAvatar(targetUser == null ? null : targetUser.getAvatar());
        view.setCreateTime(request.getCreateTime());
        view.setUpdateTime(request.getUpdateTime());
        return view;
    }

    private ChatMessageSearchView toSearchView(User currentUser, ChatMessage message) {
        Long peerUserId = Objects.equals(message.getSenderId(), currentUser.getId())
                ? message.getReceiverId()
                : message.getSenderId();
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(message.getSenderId());
        if (peerUserId != null) {
            userIds.add(peerUserId);
        }
        Map<Long, User> userMap = loadUsers(userIds);
        User peerUser = userMap.get(peerUserId);
        User sender = userMap.get(message.getSenderId());

        ChatMessageSearchView view = new ChatMessageSearchView();
        view.setId(message.getId());
        view.setPeerUserId(peerUserId);
        view.setPeerName(displayName(peerUser));
        view.setPeerAvatar(peerUser == null ? null : peerUser.getAvatar());
        view.setContent(message.getContent());
        view.setSenderId(message.getSenderId());
        view.setSenderName(displayName(sender));
        view.setSenderRole(sender == null ? null : sender.getRole());
        view.setCreateTime(message.getCreateTime());
        return view;
    }

    private List<ChatMessageView> toMessageViews(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        Set<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> userMap = loadUsers(senderIds);

        return messages.stream()
                .sorted(Comparator.comparing(ChatMessage::getCreateTime).thenComparing(ChatMessage::getId))
                .map(message -> toMessageView(message, userMap))
                .collect(Collectors.toList());
    }

    private ChatMessageView toMessageView(ChatMessage message) {
        return toMessageView(message, loadUsers(List.of(message.getSenderId())));
    }

    private ChatMessageView toMessageView(ChatMessage message, Map<Long, User> userMap) {
        User sender = userMap.get(message.getSenderId());

        ChatMessageView view = new ChatMessageView();
        view.setId(message.getId());
        view.setRoomType(message.getRoomType());
        view.setRoomKey(message.getRoomKey());
        view.setSenderId(message.getSenderId());
        view.setSenderName(displayName(sender));
        view.setSenderAvatar(sender == null ? null : sender.getAvatar());
        view.setSenderRole(sender == null ? null : sender.getRole());
        view.setReceiverId(message.getReceiverId());
        view.setContent(message.getContent());
        view.setCreateTime(message.getCreateTime());
        return view;
    }

    private void validateRequestablePair(User currentUser, User targetUser) {
        if (Objects.equals(currentUser.getId(), targetUser.getId())) {
            throw new IllegalArgumentException("不能给自己发送好友申请");
        }
        if (isAdmin(currentUser) || isAdmin(targetUser)) {
            throw new IllegalArgumentException("管理员相关会话可直接私聊，无需发起好友申请");
        }
        if (isFriend(currentUser.getId(), targetUser.getId())) {
            throw new IllegalArgumentException("你们已经是好友了");
        }
        if (isBlocked(currentUser.getId(), targetUser.getId()) || isBlocked(targetUser.getId(), currentUser.getId())) {
            throw new IllegalArgumentException("存在拉黑关系，无法发起好友申请");
        }
        if (findPendingRequest(currentUser.getId(), targetUser.getId()) != null) {
            throw new IllegalArgumentException("好友申请已发送，请等待对方处理");
        }
    }

    private ChatFriendRequest getPendingIncomingRequest(Long currentUserId, Long requestId) {
        ChatFriendRequest request = chatFriendRequestMapper.selectById(requestId);
        if (request == null) {
            return null;
        }
        if (!Objects.equals(request.getTargetUserId(), currentUserId)) {
            return null;
        }
        if (!REQUEST_STATUS_PENDING.equals(request.getStatus())) {
            return null;
        }
        return request;
    }

    private ChatFriendRequest findPendingRequest(Long requesterId, Long targetUserId) {
        List<ChatFriendRequest> requests = chatFriendRequestMapper.selectList(
                new LambdaQueryWrapper<ChatFriendRequest>()
                        .eq(ChatFriendRequest::getRequesterId, requesterId)
                        .eq(ChatFriendRequest::getTargetUserId, targetUserId)
                        .eq(ChatFriendRequest::getStatus, REQUEST_STATUS_PENDING)
                        .orderByDesc(ChatFriendRequest::getUpdateTime)
                        .orderByDesc(ChatFriendRequest::getId)
                        .last("LIMIT 1")
        );
        return requests.isEmpty() ? null : requests.get(0);
    }

    private void updateRequestStatus(ChatFriendRequest request, String status) {
        LocalDateTime now = LocalDateTime.now();
        request.setStatus(status);
        request.setHandledTime(now);
        request.setUpdateTime(now);
        chatFriendRequestMapper.updateById(request);
    }

    private void cancelPendingRequestsBetween(Long userId, Long targetUserId) {
        List<ChatFriendRequest> requests = chatFriendRequestMapper.selectList(
                new LambdaQueryWrapper<ChatFriendRequest>()
                        .eq(ChatFriendRequest::getStatus, REQUEST_STATUS_PENDING)
                        .and(wrapper -> wrapper
                                .and(inner -> inner.eq(ChatFriendRequest::getRequesterId, userId)
                                        .eq(ChatFriendRequest::getTargetUserId, targetUserId))
                                .or(inner -> inner.eq(ChatFriendRequest::getRequesterId, targetUserId)
                                        .eq(ChatFriendRequest::getTargetUserId, userId)))
        );

        for (ChatFriendRequest request : requests) {
            updateRequestStatus(request, REQUEST_STATUS_CANCELLED);
        }
    }

    private void updateGlobalRoomStates(Long senderId, String content, LocalDateTime messageTime) {
        String preview = buildPreview(content);
        List<Long> activeUserIds = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1)
        ).stream().map(User::getId).collect(Collectors.toList());

        for (Long userId : activeUserIds) {
            ChatRoomState state = getOrCreateRoomState(userId, ROOM_TYPE_GLOBAL, GLOBAL_ROOM_KEY, null);
            state.setLastMessagePreview(preview);
            state.setLastMessageTime(messageTime);
            state.setUpdateTime(messageTime);
            if (Objects.equals(userId, senderId)) {
                state.setUnreadCount(0);
                state.setLastReadTime(messageTime);
            } else {
                state.setUnreadCount((state.getUnreadCount() == null ? 0 : state.getUnreadCount()) + 1);
            }
            saveRoomState(state);
        }
    }

    private void updatePrivateRoomStates(Long senderId, Long receiverId, String roomKey, String content, LocalDateTime messageTime) {
        String preview = buildPreview(content);

        ChatRoomState senderState = getOrCreateRoomState(senderId, ROOM_TYPE_PRIVATE, roomKey, receiverId);
        senderState.setPeerUserId(receiverId);
        senderState.setUnreadCount(0);
        senderState.setLastMessagePreview(preview);
        senderState.setLastMessageTime(messageTime);
        senderState.setLastReadTime(messageTime);
        senderState.setUpdateTime(messageTime);
        saveRoomState(senderState);

        ChatRoomState receiverState = getOrCreateRoomState(receiverId, ROOM_TYPE_PRIVATE, roomKey, senderId);
        receiverState.setPeerUserId(senderId);
        receiverState.setUnreadCount((receiverState.getUnreadCount() == null ? 0 : receiverState.getUnreadCount()) + 1);
        receiverState.setLastMessagePreview(preview);
        receiverState.setLastMessageTime(messageTime);
        receiverState.setUpdateTime(messageTime);
        saveRoomState(receiverState);
    }

    private void pushGlobalMessage(ChatMessageView view) {
        chatPushService.broadcast(Map.of(
                "type", "chat.message",
                "message", view
        ));
    }

    private void pushPrivateMessage(Long senderId, Long receiverId, ChatMessageView view) {
        chatPushService.sendToUsers(List.of(senderId, receiverId), Map.of(
                "type", "chat.message",
                "message", view
        ));
    }

    private void pushSync(Long userId, Long targetUserId, String reason) {
        chatPushService.sendToUsers(List.of(userId, targetUserId), Map.of(
                "type", "chat.sync",
                "reason", reason
        ));
    }

    private int getGlobalUnreadCount(Long userId) {
        ChatRoomState state = chatRoomStateMapper.selectOne(
                new LambdaQueryWrapper<ChatRoomState>()
                        .eq(ChatRoomState::getUserId, userId)
                        .eq(ChatRoomState::getRoomType, ROOM_TYPE_GLOBAL)
                        .eq(ChatRoomState::getRoomKey, GLOBAL_ROOM_KEY)
        );
        return state == null || state.getUnreadCount() == null ? 0 : state.getUnreadCount();
    }

    private void ensureGlobalRoomState(Long userId) {
        getOrCreateRoomState(userId, ROOM_TYPE_GLOBAL, GLOBAL_ROOM_KEY, null);
    }

    private ChatRoomState getOrCreateRoomState(Long userId, String roomType, String roomKey, Long peerUserId) {
        ChatRoomState state = chatRoomStateMapper.selectOne(
                new LambdaQueryWrapper<ChatRoomState>()
                        .eq(ChatRoomState::getUserId, userId)
                        .eq(ChatRoomState::getRoomType, roomType)
                        .eq(ChatRoomState::getRoomKey, roomKey)
        );
        if (state != null) {
            return state;
        }

        LocalDateTime now = LocalDateTime.now();
        state = new ChatRoomState();
        state.setUserId(userId);
        state.setRoomType(roomType);
        state.setRoomKey(roomKey);
        state.setPeerUserId(peerUserId);
        state.setUnreadCount(0);
        state.setCreateTime(now);
        state.setUpdateTime(now);
        saveRoomState(state);
        return state;
    }

    private void saveRoomState(ChatRoomState state) {
        if (state.getId() == null) {
            chatRoomStateMapper.insert(state);
            return;
        }
        chatRoomStateMapper.updateById(state);
    }

    private boolean canOpenPrivateConversation(User currentUser, User targetUser) {
        if (currentUser == null || targetUser == null) {
            return false;
        }
        if (Objects.equals(currentUser.getId(), targetUser.getId())) {
            return false;
        }
        if (isAdmin(currentUser)) {
            return true;
        }
        if (isBlocked(currentUser.getId(), targetUser.getId()) || isBlocked(targetUser.getId(), currentUser.getId())) {
            return false;
        }
        if (isAdmin(targetUser)) {
            return true;
        }
        return isFriend(currentUser.getId(), targetUser.getId());
    }

    private boolean isFriend(Long userId, Long friendId) {
        return chatFriendMapper.selectCount(
                new LambdaQueryWrapper<ChatFriend>()
                        .eq(ChatFriend::getUserId, userId)
                        .eq(ChatFriend::getFriendId, friendId)
        ) > 0;
    }

    private boolean isBlocked(Long userId, Long blockedUserId) {
        return chatBlockMapper.selectCount(
                new LambdaQueryWrapper<ChatBlock>()
                        .eq(ChatBlock::getUserId, userId)
                        .eq(ChatBlock::getBlockedUserId, blockedUserId)
        ) > 0;
    }

    private void saveFriendPair(Long userId, Long friendId) {
        if (isFriend(userId, friendId)) {
            return;
        }
        ChatFriend friend = new ChatFriend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setCreateTime(LocalDateTime.now());
        chatFriendMapper.insert(friend);
    }

    private void deleteFriendPair(Long userId, Long friendId) {
        chatFriendMapper.delete(
                new LambdaQueryWrapper<ChatFriend>()
                        .eq(ChatFriend::getUserId, userId)
                        .eq(ChatFriend::getFriendId, friendId)
        );
    }

    private void saveBlockPair(Long userId, Long blockedUserId) {
        if (isBlocked(userId, blockedUserId)) {
            return;
        }
        ChatBlock block = new ChatBlock();
        block.setUserId(userId);
        block.setBlockedUserId(blockedUserId);
        block.setCreateTime(LocalDateTime.now());
        chatBlockMapper.insert(block);
    }

    private User getActiveUser(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getStatus(), 1)) {
            return null;
        }
        return user;
    }

    private boolean isAdmin(User user) {
        return user != null && isAdminRole(user.getRole());
    }

    private boolean isAdminRole(String role) {
        return "admin".equalsIgnoreCase(role) || "manager".equalsIgnoreCase(role);
    }

    private String buildPrivateRoomKey(Long userId, Long targetUserId) {
        long left = Math.min(userId, targetUserId);
        long right = Math.max(userId, targetUserId);
        return left + "_" + right;
    }

    private String buildPreview(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= PREVIEW_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_LIMIT) + "...";
    }

    private String displayName(User user) {
        if (user == null) {
            return "用户";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "未命名用户";
    }

    private String displayName(ChatContactView view) {
        if (view == null) {
            return "聊天";
        }
        if (view.getNickname() != null && !view.getNickname().isBlank()) {
            return view.getNickname();
        }
        if (view.getUsername() != null && !view.getUsername().isBlank()) {
            return view.getUsername();
        }
        return "未命名用户";
    }

    private Map<Long, User> loadUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
    }
}
