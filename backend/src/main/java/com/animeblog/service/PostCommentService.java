package com.animeblog.service;

import com.animeblog.dto.AdminPageResult;
import com.animeblog.dto.AdminPostCommentView;
import com.animeblog.dto.PostCommentCreateRequest;
import com.animeblog.dto.PostCommentSubmitResponse;
import com.animeblog.dto.PostCommentView;
import com.animeblog.entity.Post;
import com.animeblog.entity.PostComment;
import com.animeblog.entity.User;
import com.animeblog.exception.BusinessException;
import com.animeblog.mapper.PostCommentMapper;
import com.animeblog.mapper.PostMapper;
import com.animeblog.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PostCommentService {

    private static final String STATUS_VISIBLE = "visible";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_REJECTED = "rejected";
    private static final String CLIENT_OS_UNKNOWN = "unknown";
    private static final String CLIENT_BROWSER_UNKNOWN = "unknown";
    private static final String ANONYMOUS_ALIAS_PATTERN = "^[\\p{IsHan}]{3,5}$";
    private static final String ANONYMOUS_SURNAME_POOL = "赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦许何吕张孔曹严华金魏陶姜戚谢邹喻柏水窦章云苏潘葛奚范彭郎鲁韦昌马苗凤花方俞任袁柳鲍史唐薛雷贺倪汤滕殷罗毕郝邬安常乐于时傅皮卞齐康伍余元顾孟平黄穆萧尹";
    private static final String ANONYMOUS_GIVEN_NAME_POOL = "安白川初灯朵恩风归海禾纪景柚林洛梦南念宁沐清秋然若山时书棠晚希夏晓星言悠雨月枝知舟竹子霁岚槿澄遥栀禾语微青鸢霖昭宁冉听澈眠寻鹿漫暖舒念泠岑朔吟可";
    private static final String PUBLIC_ANONYMOUS_NAME = "匿名访客";
    private static final int MAX_COMMENT_AVATAR_LENGTH = 60000;
    private static final Comparator<PostComment> ROOT_COMMENT_ORDER = Comparator
            .comparing(PostComment::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PostComment::getId, Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed();
    private static final Comparator<PostComment> REPLY_COMMENT_ORDER = Comparator
            .comparing(PostComment::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PostComment::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentIpLocationService commentIpLocationService;

    public List<PostCommentView> getVisibleCommentsByPostId(Long postId) {
        ensurePublishedPost(postId);

        QueryWrapper<PostComment> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", postId)
                .eq("status", STATUS_VISIBLE)
                .orderByAsc("create_time", "id");

        List<PostComment> comments = postCommentMapper.selectList(wrapper);
        return buildPublicCommentTree(comments);
    }

    public PostCommentSubmitResponse createComment(
            Long postId,
            PostCommentCreateRequest request,
            User currentUser,
            String clientIp,
            String userAgent
    ) {
        ensurePublishedPost(postId);

        String content = normalizeContent(request.getContent());
        List<String> images = normalizeImages(request.getImages());
        if (!StringUtils.hasText(content) && images.isEmpty()) {
            throw new BusinessException("评论内容和图片不能同时为空");
        }

        PostComment parentComment = resolveReplyParent(postId, request.getParentId());
        boolean guestMode = currentUser == null || Boolean.TRUE.equals(request.getGuestMode());
        CommentIdentity identity = resolveIdentity(request, currentUser, guestMode);
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        String status = anonymous ? STATUS_PENDING : STATUS_VISIBLE;
        LocalDateTime now = LocalDateTime.now();

        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(guestMode ? null : currentUser.getId());
        comment.setParentId(parentComment == null ? null : parentComment.getId());
        comment.setAuthorName(identity.authorName());
        comment.setAuthorEmail(identity.authorEmail());
        comment.setAuthorWebsite(identity.authorWebsite());
        comment.setAuthorAvatar(resolveAuthorAvatar(currentUser, guestMode, anonymous));
        comment.setContent(content);
        comment.setImages(writeImages(images));
        comment.setLikeCount(0L);
        comment.setAnonymous(anonymous ? 1 : 0);
        comment.setStatus(status);
        comment.setReviewedBy(null);
        comment.setReviewNote(null);
        comment.setReviewedTime(null);
        comment.setClientIp(trimToNull(clientIp, 50));
        comment.setUserAgent(trimToNull(userAgent, 255));
        comment.setCreateTime(now);
        comment.setUpdateTime(now);

        postCommentMapper.insert(comment);

        if (STATUS_VISIBLE.equals(status)) {
            updatePostCommentCount(postId, 1);
        }

        Map<Long, PostComment> commentMap = new LinkedHashMap<>();
        if (parentComment != null) {
            commentMap.put(parentComment.getId(), parentComment);
        }
        commentMap.put(comment.getId(), comment);
        Map<Long, User> userMap = buildUserMap(commentMap.values(), Collections.emptyList());

        PostCommentSubmitResponse response = new PostCommentSubmitResponse();
        response.setStatus(status);
        response.setReviewRequired(anonymous);
        response.setMessage(anonymous ? "匿名评论已提交，等待管理员审核" : "评论已发布");
        response.setComment(toPublicView(comment, commentMap, Collections.emptyMap(), userMap, false));
        return response;
    }

    public List<AdminPostCommentView> getCommentsForAdmin(String status) {
        return buildAdminComments(status, null, null);
    }

    public AdminPageResult<AdminPostCommentView> getCommentsForAdmin(
            String status,
            String keyword,
            Long postId,
            int page,
            int size
    ) {
        List<AdminPostCommentView> scopedComments = buildAdminComments("all", keyword, postId);
        List<AdminPostCommentView> filteredComments = scopedComments.stream()
                .filter(item -> matchesCommentStatus(item, status))
                .toList();
        return paginate(filteredComments, page, size, summarizeComments(scopedComments));
    }

    private List<AdminPostCommentView> buildAdminComments(String status, String keyword, Long postId) {
        QueryWrapper<PostComment> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq("status", status.trim());
        }
        wrapper.orderByDesc("create_time", "id");

        List<PostComment> comments = postCommentMapper.selectList(wrapper);
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> parentIds = comments.stream()
                .map(PostComment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, PostComment> parentMap = parentIds.isEmpty()
                ? Collections.emptyMap()
                : postCommentMapper.selectBatchIds(parentIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(PostComment::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Set<Long> postIds = comments.stream()
                .map(PostComment::getPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Post> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : postMapper.selectBatchIds(postIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(Post::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<Long, User> userMap = buildUserMap(comments, parentMap.values());

        List<AdminPostCommentView> result = new ArrayList<>(comments.size());
        for (PostComment comment : comments) {
            AdminPostCommentView view = new AdminPostCommentView();
            Post post = comment.getPostId() == null ? null : postMap.get(comment.getPostId());
            PostComment parentComment = comment.getParentId() == null ? null : parentMap.get(comment.getParentId());
            User authorUser = comment.getUserId() == null ? null : userMap.get(comment.getUserId());
            User reviewer = comment.getReviewedBy() == null ? null : userMap.get(comment.getReviewedBy());

            view.setId(comment.getId());
            view.setPostId(comment.getPostId());
            view.setPostTitle(post == null ? "" : defaultString(post.getTitle()));
            view.setParentId(comment.getParentId());
            view.setParentAuthorName(parentComment == null ? "" : defaultString(parentComment.getAuthorName()));
            view.setParentContent(parentComment == null ? "" : defaultString(parentComment.getContent()));
            view.setParentStatus(parentComment == null ? "" : defaultString(parentComment.getStatus()));
            view.setParentAnonymous(parentComment != null && isAnonymous(parentComment));
            view.setUserId(comment.getUserId());
            view.setUsername(authorUser == null ? "" : defaultString(authorUser.getUsername()));
            view.setNickname(authorUser == null ? "" : defaultString(authorUser.getNickname()));
            view.setAuthorName(defaultString(comment.getAuthorName()));
            view.setAuthorEmail(defaultString(comment.getAuthorEmail()));
            view.setAuthorWebsite(defaultString(comment.getAuthorWebsite()));
            view.setAuthorAvatar(defaultString(comment.getAuthorAvatar()));
            view.setAuthorRole(authorUser == null ? "" : defaultString(authorUser.getRole()));
            view.setAnonymous(isAnonymous(comment));
            view.setStatus(defaultString(comment.getStatus()));
            view.setClientRegion(resolveClientRegion(comment.getClientIp()));
            view.setClientOs(resolveClientOs(comment.getUserAgent()));
            view.setClientBrowser(resolveClientBrowser(comment.getUserAgent()));
            view.setContent(defaultString(comment.getContent()));
            view.setImages(readImages(comment.getImages()));
            view.setLikeCount(comment.getLikeCount() == null ? 0L : comment.getLikeCount());
            view.setReviewedBy(comment.getReviewedBy());
            view.setReviewerName(reviewer == null ? "" : resolveReviewerName(reviewer));
            view.setReviewNote(defaultString(comment.getReviewNote()));
            view.setReviewedAt(comment.getReviewedTime());
            view.setCreatedAt(comment.getCreateTime());
            result.add(view);
        }
        return result.stream()
                .filter(item -> matchesCommentPost(item, postId))
                .filter(item -> matchesCommentKeyword(item, keyword))
                .toList();
    }

    public AdminPostCommentView approveComment(Long commentId, User reviewer) {
        PostComment comment = requireComment(commentId);
        if (!STATUS_PENDING.equals(comment.getStatus())) {
            throw new BusinessException("只有待审核评论可以通过");
        }

        comment.setStatus(STATUS_VISIBLE);
        comment.setReviewedBy(reviewer == null ? null : reviewer.getId());
        comment.setReviewNote(null);
        comment.setReviewedTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        postCommentMapper.updateById(comment);
        updatePostCommentCount(comment.getPostId(), 1);

        return getSingleAdminComment(comment.getId());
    }

    public AdminPostCommentView rejectComment(Long commentId, User reviewer, String reviewNote) {
        PostComment comment = requireComment(commentId);
        if (!STATUS_PENDING.equals(comment.getStatus())) {
            throw new BusinessException("只有待审核评论可以驳回");
        }

        String normalizedNote = trimToNull(reviewNote, 500);
        if (!StringUtils.hasText(normalizedNote)) {
            throw new BusinessException("请填写驳回原因");
        }

        comment.setStatus(STATUS_REJECTED);
        comment.setReviewedBy(reviewer == null ? null : reviewer.getId());
        comment.setReviewNote(normalizedNote);
        comment.setReviewedTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        postCommentMapper.updateById(comment);

        return getSingleAdminComment(comment.getId());
    }

    public void deleteComment(Long commentId) {
        deleteCommentAndReturnIds(commentId);
    }

    public Set<Long> deleteCommentAndReturnIds(Long commentId) {
        PostComment target = postCommentMapper.selectById(commentId);
        if (target == null) {
            return Collections.emptySet();
        }

        QueryWrapper<PostComment> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", target.getPostId());
        List<PostComment> postComments = postCommentMapper.selectList(wrapper);
        if (postComments.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> deleteIds = collectCommentTreeIds(target.getId(), postComments);
        if (deleteIds.isEmpty()) {
            return Collections.emptySet();
        }

        long visibleCount = postComments.stream()
                .filter(comment -> deleteIds.contains(comment.getId()))
                .filter(comment -> STATUS_VISIBLE.equals(comment.getStatus()))
                .count();

        postCommentMapper.deleteBatchIds(deleteIds);
        if (visibleCount > 0) {
            updatePostCommentCount(target.getPostId(), -visibleCount);
        }

        return deleteIds;
    }

    private AdminPostCommentView getSingleAdminComment(Long commentId) {
        return buildAdminComments("all", null, null).stream()
                .filter(item -> Objects.equals(item.getId(), commentId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("评论不存在"));
    }

    private PostComment requireComment(Long commentId) {
        if (commentId == null) {
            throw new BusinessException("评论不存在");
        }
        PostComment comment = postCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        return comment;
    }

    private PostComment resolveReplyParent(Long postId, Long parentId) {
        if (parentId == null) {
            return null;
        }

        PostComment parent = postCommentMapper.selectById(parentId);
        if (parent == null || !Objects.equals(parent.getPostId(), postId)) {
            throw new BusinessException("回复目标不存在");
        }
        if (!STATUS_VISIBLE.equals(parent.getStatus())) {
            throw new BusinessException("当前回复目标不可见，暂时无法回复");
        }
        return parent;
    }

    private void ensurePublishedPost(Long postId) {
        if (postId == null) {
            throw new BusinessException("文章不存在");
        }

        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("id", postId).eq("status", "published");
        Post post = postMapper.selectOne(wrapper);
        if (post == null) {
            throw new BusinessException("文章不存在或未发布");
        }
    }

    private void updatePostCommentCount(Long postId, long delta) {
        if (postId == null || delta == 0) {
            return;
        }

        UpdateWrapper<Post> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", postId);
        if (delta > 0) {
            wrapper.setSql("comment_count = COALESCE(comment_count, 0) + " + delta);
        } else {
            wrapper.setSql("comment_count = GREATEST(COALESCE(comment_count, 0) - " + Math.abs(delta) + ", 0)");
        }
        postMapper.update(null, wrapper);
    }

    private List<PostCommentView> buildPublicCommentTree(List<PostComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, PostComment> commentMap = comments.stream()
                .collect(Collectors.toMap(PostComment::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<PostComment>> childrenByParent = comments.stream()
                .filter(comment -> comment.getParentId() != null)
                .collect(Collectors.groupingBy(PostComment::getParentId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, User> userMap = buildUserMap(comments, Collections.emptyList());

        return comments.stream()
                .filter(comment -> comment.getParentId() == null)
                .sorted(ROOT_COMMENT_ORDER)
                .map(comment -> toPublicView(comment, commentMap, childrenByParent, userMap, true))
                .collect(Collectors.toList());
    }

    private PostCommentView toPublicView(
            PostComment comment,
            Map<Long, PostComment> commentMap,
            Map<Long, List<PostComment>> childrenByParent,
            Map<Long, User> userMap,
            boolean includeReplies
    ) {
        PostCommentView view = new PostCommentView();
        view.setId(comment.getId());
        view.setPostId(comment.getPostId());
        view.setParentId(comment.getParentId());
        view.setAuthorName(resolvePublicAuthorName(comment));
        view.setAuthorAvatar(isAnonymous(comment) ? "" : defaultString(comment.getAuthorAvatar()));
        view.setAuthorRole(resolveAuthorRole(comment, userMap));
        view.setAuthorWebsite(isAnonymous(comment) ? "" : defaultString(comment.getAuthorWebsite()));
        view.setAnonymous(isAnonymous(comment));
        view.setStatus(defaultString(comment.getStatus()));
        view.setClientRegion(resolveClientRegion(comment.getClientIp()));
        view.setClientOs(resolveClientOs(comment.getUserAgent()));
        view.setClientBrowser(resolveClientBrowser(comment.getUserAgent()));
        view.setContent(defaultString(comment.getContent()));
        view.setImages(readImages(comment.getImages()));
        view.setLikeCount(comment.getLikeCount() == null ? 0L : comment.getLikeCount());
        view.setCreatedAt(comment.getCreateTime());
        view.setReplyToAuthorName(resolveReplyTargetName(comment, commentMap));

        if (!includeReplies) {
            view.setReplies(Collections.emptyList());
            return view;
        }

        List<PostComment> replies = childrenByParent.getOrDefault(comment.getId(), Collections.emptyList()).stream()
                .sorted(REPLY_COMMENT_ORDER)
                .collect(Collectors.toList());
        List<PostCommentView> replyViews = replies.stream()
                .map(item -> toPublicView(item, commentMap, childrenByParent, userMap, true))
                .collect(Collectors.toList());
        view.setReplies(replyViews);
        return view;
    }

    private Map<Long, User> buildUserMap(Collection<PostComment> comments, Collection<PostComment> extraComments) {
        Set<Long> userIds = new LinkedHashSet<>();
        collectUserIds(userIds, comments);
        collectUserIds(userIds, extraComments);

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private void collectUserIds(Set<Long> userIds, Collection<PostComment> comments) {
        if (comments == null) {
            return;
        }
        for (PostComment comment : comments) {
            if (comment == null) {
                continue;
            }
            if (comment.getUserId() != null) {
                userIds.add(comment.getUserId());
            }
            if (comment.getReviewedBy() != null) {
                userIds.add(comment.getReviewedBy());
            }
        }
    }

    private String resolveReplyTargetName(PostComment comment, Map<Long, PostComment> commentMap) {
        if (comment.getParentId() == null) {
            return "";
        }
        PostComment parent = commentMap.get(comment.getParentId());
        if (parent == null) {
            return "";
        }
        return resolvePublicAuthorName(parent);
    }

    private String resolvePublicAuthorName(PostComment comment) {
        if (isAnonymous(comment)) {
            String anonymousAlias = normalizeAnonymousAlias(comment.getAuthorName());
            return StringUtils.hasText(anonymousAlias) ? anonymousAlias : PUBLIC_ANONYMOUS_NAME;
        }
        return defaultString(comment.getAuthorName());
    }

    private String resolveAuthorRole(PostComment comment, Map<Long, User> userMap) {
        if (isAnonymous(comment) || comment.getUserId() == null) {
            return "";
        }
        User user = userMap.get(comment.getUserId());
        if (user == null || !StringUtils.hasText(user.getRole())) {
            return "";
        }
        return user.getRole();
    }

    private String resolveReviewerName(User reviewer) {
        String nickname = trimToNull(reviewer.getNickname(), 100);
        if (StringUtils.hasText(nickname)) {
            return nickname;
        }
        return defaultString(reviewer.getUsername());
    }

    private String resolveAuthorAvatar(User currentUser, boolean guestMode, boolean anonymous) {
        if (guestMode || anonymous || currentUser == null) {
            return "";
        }
        return trimToNull(currentUser.getAvatar(), MAX_COMMENT_AVATAR_LENGTH);
    }

    private CommentIdentity resolveIdentity(PostCommentCreateRequest request, User currentUser, boolean guestMode) {
        boolean anonymous = Boolean.TRUE.equals(request.getAnonymous());
        if (!guestMode) {
            if (currentUser == null) {
                throw new BusinessException("请先登录，或切换到其他方式填写昵称和邮箱");
            }

            String displayName = anonymous ? normalizeAnonymousAlias(request.getAuthorName()) : trimToNull(currentUser.getNickname(), 100);
            if (!StringUtils.hasText(displayName) && !anonymous) {
                displayName = trimToNull(currentUser.getUsername(), 100);
            }
            if (!StringUtils.hasText(displayName) && anonymous) {
                displayName = generateAnonymousAlias();
            }
            if (!StringUtils.hasText(displayName)) {
                throw new BusinessException("当前账号缺少可显示昵称，请使用其他方式发表评论");
            }

            return new CommentIdentity(
                    displayName,
                    normalizeEmail(currentUser.getEmail()),
                    null
            );
        }

        String authorName = anonymous ? normalizeAnonymousAlias(request.getAuthorName()) : trimToNull(request.getAuthorName(), 100);
        String authorEmail = normalizeEmail(request.getAuthorEmail());
        String authorWebsite = normalizeWebsite(request.getAuthorWebsite());

        if (anonymous && !StringUtils.hasText(authorName)) {
            authorName = generateAnonymousAlias();
        }

        if (!StringUtils.hasText(authorName)) {
            throw new BusinessException("请填写昵称");
        }
        if (!anonymous && !StringUtils.hasText(authorEmail)) {
            throw new BusinessException("请填写邮箱");
        }

        return new CommentIdentity(authorName, authorEmail, authorWebsite);
    }

    private boolean isAnonymous(PostComment comment) {
        return comment.getAnonymous() != null && comment.getAnonymous() == 1;
    }

    private Set<Long> collectCommentTreeIds(Long rootId, List<PostComment> comments) {
        if (rootId == null || comments == null || comments.isEmpty()) {
            return Collections.emptySet();
        }

        Map<Long, List<PostComment>> childrenByParent = comments.stream()
                .filter(comment -> comment.getParentId() != null)
                .collect(Collectors.groupingBy(PostComment::getParentId, LinkedHashMap::new, Collectors.toList()));

        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);

        while (!queue.isEmpty()) {
            Long currentId = queue.removeFirst();
            if (!result.add(currentId)) {
                continue;
            }
            for (PostComment child : childrenByParent.getOrDefault(currentId, Collections.emptyList())) {
                if (child.getId() != null) {
                    queue.addLast(child.getId());
                }
            }
        }

        return result;
    }

    private String normalizeContent(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (normalized.length() > 100000) {
            throw new BusinessException("评论内容不能超过 100000 个字符");
        }
        return normalized;
    }

    private List<String> normalizeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }

        return images.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .peek(value -> {
                    if (!isAllowedImageUrl(value)) {
                        throw new BusinessException("评论图片地址格式不正确");
                    }
                })
                .distinct()
                .limit(9)
                .collect(Collectors.toList());
    }

    private boolean isAllowedImageUrl(String value) {
        return value.startsWith("http://")
                || value.startsWith("https://")
                || value.startsWith("/uploads/")
                || value.startsWith("data:image/");
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value, 100);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeWebsite(String value) {
        String normalized = trimToNull(value, 255);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }

        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Unsupported scheme");
            }
            if (!StringUtils.hasText(uri.getHost())) {
                throw new IllegalArgumentException("Missing host");
            }
            return normalized;
        } catch (Exception e) {
            throw new BusinessException("网站地址格式不正确");
        }
    }

    private String writeImages(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images == null ? Collections.emptyList() : images);
        } catch (Exception e) {
            throw new BusinessException("评论图片保存失败", e);
        }
    }

    private List<String> readImages(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        try {
            List<String> images = objectMapper.readValue(value, new TypeReference<List<String>>() {});
            return images == null ? Collections.emptyList() : images;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String trimToNull(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            normalized = normalized.substring(0, maxLength);
        }
        return normalized;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String normalizeAnonymousAlias(String value) {
        String normalized = trimToNull(value, 5);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.matches(ANONYMOUS_ALIAS_PATTERN) ? normalized : null;
    }

    private String generateAnonymousAlias() {
        String surname = String.valueOf(ANONYMOUS_SURNAME_POOL.charAt((int) (Math.random() * ANONYMOUS_SURNAME_POOL.length())));
        int givenLength = 2 + (int) (Math.random() * 3);
        StringBuilder builder = new StringBuilder(surname);
        for (int index = 0; index < givenLength; index++) {
            builder.append(ANONYMOUS_GIVEN_NAME_POOL.charAt((int) (Math.random() * ANONYMOUS_GIVEN_NAME_POOL.length())));
        }
        return builder.substring(0, Math.min(builder.length(), 5));
    }

    private String resolveClientRegion(String clientIp) {
        return commentIpLocationService.resolveRegion(clientIp);
    }

    private String resolveClientOs(String userAgent) {
        String normalizedUserAgent = normalizeUserAgent(userAgent);
        if (!StringUtils.hasText(normalizedUserAgent)) {
            return CLIENT_OS_UNKNOWN;
        }

        if (normalizedUserAgent.contains("harmonyos")) {
            return "harmonyos";
        }
        if (normalizedUserAgent.contains("iphone") || normalizedUserAgent.contains("ipod")) {
            return "ios";
        }
        if (normalizedUserAgent.contains("ipad")) {
            return "ipados";
        }
        if (normalizedUserAgent.contains("android")) {
            return "android";
        }
        if (normalizedUserAgent.contains("windows")) {
            return "windows";
        }
        if (normalizedUserAgent.contains("mac os x") || normalizedUserAgent.contains("macintosh")) {
            return "macos";
        }
        if (normalizedUserAgent.contains("cros")) {
            return "chromeos";
        }
        if (normalizedUserAgent.contains("linux")) {
            return "linux";
        }
        if (normalizedUserAgent.contains("x11")) {
            return "unix";
        }
        return CLIENT_OS_UNKNOWN;
    }

    private String resolveClientBrowser(String userAgent) {
        String normalizedUserAgent = normalizeUserAgent(userAgent);
        if (!StringUtils.hasText(normalizedUserAgent)) {
            return CLIENT_BROWSER_UNKNOWN;
        }

        if (normalizedUserAgent.contains("micromessenger")) {
            return "wechat";
        }
        if (normalizedUserAgent.contains("qqbrowser")) {
            return "qq-browser";
        }
        if (normalizedUserAgent.contains("samsungbrowser")) {
            return "samsung-internet";
        }
        if (normalizedUserAgent.contains("edg/") || normalizedUserAgent.contains("edge/")) {
            return "edge";
        }
        if (normalizedUserAgent.contains("opr/") || normalizedUserAgent.contains("opera")) {
            return "opera";
        }
        if (normalizedUserAgent.contains("firefox/")) {
            return "firefox";
        }
        if (normalizedUserAgent.contains("msie") || normalizedUserAgent.contains("trident/")) {
            return "ie";
        }
        if (normalizedUserAgent.contains("chrome/") || normalizedUserAgent.contains("crios/")) {
            return "chrome";
        }
        if (normalizedUserAgent.contains("safari/")) {
            return "safari";
        }
        return CLIENT_BROWSER_UNKNOWN;
    }

    private boolean matchesCommentStatus(AdminPostCommentView item, String status) {
        if (item == null) {
            return false;
        }
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return true;
        }
        return status.trim().equalsIgnoreCase(item.getStatus());
    }

    private boolean matchesCommentPost(AdminPostCommentView item, Long postId) {
        if (postId == null) {
            return true;
        }
        return Objects.equals(item.getPostId(), postId);
    }

    private boolean matchesCommentKeyword(AdminPostCommentView item, String keyword) {
        String normalizedKeyword = trimToNull(keyword, 100);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }

        String searchableText = String.join("\n",
                defaultString(item.getPostTitle()),
                defaultString(item.getAuthorName()),
                defaultString(item.getNickname()),
                defaultString(item.getUsername()),
                defaultString(item.getAuthorEmail()),
                defaultString(item.getContent()),
                defaultString(item.getParentAuthorName()),
                defaultString(item.getParentContent()),
                item.getPostId() == null ? "" : String.valueOf(item.getPostId())
        ).toLowerCase(Locale.ROOT);

        return searchableText.contains(normalizedKeyword.toLowerCase(Locale.ROOT));
    }

    private Map<String, Long> summarizeComments(List<AdminPostCommentView> items) {
        long pending = items.stream().filter(item -> STATUS_PENDING.equals(item.getStatus())).count();
        long visible = items.stream().filter(item -> STATUS_VISIBLE.equals(item.getStatus())).count();
        long rejected = items.stream().filter(item -> STATUS_REJECTED.equals(item.getStatus())).count();

        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("total", (long) items.size());
        summary.put("pending", pending);
        summary.put("visible", visible);
        summary.put("rejected", rejected);
        return summary;
    }

    private <T> AdminPageResult<T> paginate(List<T> items, int page, int size, Map<String, Long> summary) {
        long current = Math.max(1L, page);
        long pageSize = Math.max(1L, size);
        int fromIndex = (int) Math.min((current - 1L) * pageSize, items.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, items.size());
        return AdminPageResult.of(items.subList(fromIndex, toIndex), current, pageSize, items.size(), summary);
    }

    private String normalizeUserAgent(String userAgent) {
        return StringUtils.hasText(userAgent) ? userAgent.toLowerCase(Locale.ROOT) : "";
    }

    private record CommentIdentity(String authorName, String authorEmail, String authorWebsite) {
    }
}
