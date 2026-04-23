package com.animeblog.service;

import com.animeblog.dto.AdminPageResult;
import com.animeblog.dto.PostReviewView;
import com.animeblog.dto.PostWorkspaceItem;
import com.animeblog.entity.Post;
import com.animeblog.entity.PostReview;
import com.animeblog.entity.User;
import com.animeblog.mapper.PostMapper;
import com.animeblog.mapper.PostReviewMapper;
import com.animeblog.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章审核服务
 * 管理文章的审核流程,包括:
 * - 用户创建/编辑文章的草稿管理
 * - 提交审核与审核通过/驳回
 * - 已发布文章的更新审核
 * - 审核工作区管理(展示用户的文章和审核记录)
 */
@Service
public class PostReviewService {

    @Autowired
    private PostReviewMapper postReviewMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostService postService;

    /**
     * 获取用户的工作区
     * 包含用户的所有文章和可编辑的审核记录
     * 用于前端展示用户的文章管理界面
     *
     * @param currentUser 当前登录用户
     * @return 工作区项目列表
     */
    public List<PostWorkspaceItem> getWorkspace(User currentUser) {
        QueryWrapper<Post> postWrapper = new QueryWrapper<>();
        postWrapper.eq("user_id", currentUser.getId()).orderByDesc("update_time").orderByDesc("create_time");
        List<Post> posts = postMapper.selectList(postWrapper);

        QueryWrapper<PostReview> reviewWrapper = new QueryWrapper<>();
        reviewWrapper.eq("user_id", currentUser.getId()).orderByDesc("update_time").orderByDesc("create_time");
        List<PostReview> reviews = postReviewMapper.selectList(reviewWrapper);

        Map<Long, PostReview> latestEditableReviewByPostId = new HashMap<>();
        List<PostWorkspaceItem> items = new ArrayList<>();

        for (PostReview review : reviews) {
            if (review.getPostId() == null) {
                if (isEditableReview(review)) {
                    items.add(toWorkspaceItem(review));
                }
                continue;
            }
            if (isEditableReview(review)) {
                latestEditableReviewByPostId.putIfAbsent(review.getPostId(), review);
            }
        }

        for (Post post : posts) {
            items.add(toWorkspaceItem(post, latestEditableReviewByPostId.get(post.getId())));
        }

        items.sort(Comparator.comparing(PostWorkspaceItem::getDisplayTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    public AdminPageResult<PostWorkspaceItem> getWorkspacePage(User currentUser, int page, int size) {
        List<PostWorkspaceItem> items = getWorkspace(currentUser);
        return paginate(items, page, size, summarizeWorkspace(items));
    }

    public Map<String, Long> getWorkspaceSummary(User currentUser) {
        return summarizeWorkspace(getWorkspace(currentUser));
    }

    public AdminPageResult<PostReviewView> getReviewsForAdmin(String status, int page, int size) {
        List<PostReviewView> reviews = buildAdminReviews();
        List<PostReviewView> filtered = reviews.stream()
                .filter(item -> matchesReviewStatus(item, status))
                .toList();
        return paginate(filtered, page, size, summarizeReviews(reviews));
    }

    private List<PostReviewView> buildAdminReviews() {
        QueryWrapper<PostReview> wrapper = new QueryWrapper<>();
        wrapper.ne("review_status", "draft");
        wrapper.ne("review_status", "withdrawn");
        wrapper.orderByDesc("update_time").orderByDesc("create_time");
        List<PostReview> reviews = postReviewMapper.selectList(wrapper);

        Set<Long> userIds = new HashSet<>();
        for (PostReview review : reviews) {
            if (review.getUserId() != null) {
                userIds.add(review.getUserId());
            }
            if (review.getReviewerId() != null) {
                userIds.add(review.getReviewerId());
            }
        }

        Map<Long, User> userMap = userIds.isEmpty()
                ? new HashMap<>()
                : userMapper.selectByIds(userIds).stream().collect(Collectors.toMap(User::getId, user -> user));

        List<PostReviewView> result = new ArrayList<>();
        for (PostReview review : reviews) {
            result.add(toReviewView(review, userMap));
        }
        result.sort(Comparator
                .comparing((PostReviewView view) -> reviewStatusOrder(view.getReviewStatus()))
                .thenComparing(PostReviewView::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public PostReview createDraftForNewPost(User currentUser, Post payload) {
        PostReview review = new PostReview();
        review.setUserId(currentUser.getId());
        review.setAction("create");
        fillReviewSnapshot(review, payload);
        LocalDateTime now = LocalDateTime.now();
        review.setPostStatus("draft");
        review.setReviewStatus("draft");
        review.setReviewNote(null);
        review.setReviewerId(null);
        review.setReviewedTime(null);
        review.setCreateTime(now);
        review.setUpdateTime(now);
        postReviewMapper.insert(review);
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public PostReview saveOwnReviewDraft(Long reviewId, User currentUser, Post payload) {
        PostReview review = postReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new IllegalArgumentException("审核记录不存在");
        }
        if (!Objects.equals(review.getUserId(), currentUser.getId())) {
            throw new IllegalArgumentException("无权编辑该审核记录");
        }
        if ("approved".equals(review.getReviewStatus())) {
            throw new IllegalArgumentException("审核已通过，无法再次编辑");
        }

        fillReviewSnapshot(review, payload);
        review.setPostStatus("draft");
        review.setReviewStatus("draft");
        review.setReviewNote(null);
        review.setReviewerId(null);
        review.setReviewedTime(null);
        review.setUpdateTime(LocalDateTime.now());
        postReviewMapper.updateById(review);
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public PostReview submitOwnDraftReview(Long reviewId, User currentUser) {
        PostReview review = postReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new IllegalArgumentException("审核记录不存在");
        }
        if (!Objects.equals(review.getUserId(), currentUser.getId())) {
            throw new IllegalArgumentException("无权提交该审核记录");
        }
        if ("approved".equals(review.getReviewStatus())) {
            throw new IllegalArgumentException("审核已通过，无需再次提交");
        }

        review.setPostStatus("published");
        review.setReviewStatus("pending");
        review.setReviewNote(null);
        review.setReviewerId(null);
        review.setReviewedTime(null);
        review.setUpdateTime(LocalDateTime.now());
        postReviewMapper.updateById(review);
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public PostReview savePostUpdateDraft(Long postId, User currentUser, Post payload) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!Objects.equals(post.getUserId(), currentUser.getId())) {
            throw new IllegalArgumentException("无权修改该文章");
        }

        QueryWrapper<PostReview> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", postId)
                .eq("user_id", currentUser.getId())
                .in("review_status", "draft", "withdrawn", "pending", "rejected")
                .orderByDesc("update_time")
                .last("LIMIT 1");

        PostReview review = postReviewMapper.selectOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        if (review == null) {
            review = new PostReview();
            review.setPostId(postId);
            review.setUserId(currentUser.getId());
            review.setAction("update");
            review.setCreateTime(now);
        }

        fillReviewSnapshot(review, payload);
        review.setPostStatus(post.getStatus());
        review.setReviewStatus("draft");
        review.setReviewNote(null);
        review.setReviewerId(null);
        review.setReviewedTime(null);
        review.setUpdateTime(now);

        if (review.getId() == null) {
            postReviewMapper.insert(review);
        } else {
            postReviewMapper.updateById(review);
        }
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public Post offlineOwnPost(Long postId, User currentUser) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!Objects.equals(post.getUserId(), currentUser.getId())) {
            throw new IllegalArgumentException("无权修改该文章");
        }

        post.setStatus("draft");
        post.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(post);
        postService.invalidatePublicCaches();
        return post;
    }

    @Transactional(rollbackFor = Exception.class)
    public Post publishOwnPost(Long postId, User currentUser) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!Objects.equals(post.getUserId(), currentUser.getId())) {
            throw new IllegalArgumentException("无权修改该文章");
        }

        post.setStatus("published");
        post.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(post);
        postService.invalidatePublicCaches();
        return post;
    }

    @Transactional(rollbackFor = Exception.class)
    public PostReview submitPostUpdateReview(Long postId, User currentUser, Post payload) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        if (!Objects.equals(post.getUserId(), currentUser.getId())) {
            throw new IllegalArgumentException("无权修改该文章");
        }

        QueryWrapper<PostReview> wrapper = new QueryWrapper<>();
        wrapper.eq("post_id", postId)
                .eq("user_id", currentUser.getId())
                .in("review_status", "draft", "withdrawn", "pending", "rejected")
                .orderByDesc("update_time")
                .last("LIMIT 1");

        PostReview review = postReviewMapper.selectOne(wrapper);
        LocalDateTime now = LocalDateTime.now();
        if (review == null) {
            review = new PostReview();
            review.setPostId(postId);
            review.setUserId(currentUser.getId());
            review.setAction("update");
            review.setCreateTime(now);
        }

        fillReviewSnapshot(review, payload);
        review.setReviewStatus("pending");
        review.setReviewNote(null);
        review.setReviewerId(null);
        review.setReviewedTime(null);
        review.setUpdateTime(now);

        if (review.getId() == null) {
            postReviewMapper.insert(review);
        } else {
            postReviewMapper.updateById(review);
        }
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public PostReview approveReview(Long reviewId, User adminUser) {
        PostReview review = postReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new IllegalArgumentException("审核记录不存在");
        }
        if ("approved".equals(review.getReviewStatus())) {
            throw new IllegalArgumentException("该审核记录已通过");
        }

        if ("create".equals(review.getAction())) {
            Post post = new Post();
            fillPostSnapshot(post, review);
            post.setUserId(review.getUserId());
            post.setViewCount(0L);
            post.setLikeCount(0L);
            post.setCommentCount(0L);
            post.setIsFeatured(0);
            post.setCreateTime(LocalDateTime.now());
            post.setUpdateTime(LocalDateTime.now());
            postMapper.insert(post);
            review.setPostId(post.getId());
        } else if ("update".equals(review.getAction())) {
            Post post = postMapper.selectById(review.getPostId());
            if (post == null) {
                throw new IllegalArgumentException("原文章不存在，无法审核通过");
            }
            fillPostSnapshot(post, review);
            post.setUpdateTime(LocalDateTime.now());
            postMapper.updateById(post);
        } else {
            throw new IllegalArgumentException("未知的审核类型");
        }

        review.setReviewStatus("approved");
        review.setReviewNote(null);
        review.setReviewerId(adminUser.getId());
        review.setReviewedTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        postReviewMapper.updateById(review);
        postService.invalidatePublicCaches();
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public PostReview rejectReview(Long reviewId, User adminUser, String reviewNote) {
        PostReview review = postReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new IllegalArgumentException("审核记录不存在");
        }
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new IllegalArgumentException("请填写驳回原因");
        }

        review.setReviewStatus("rejected");
        review.setReviewNote(reviewNote.trim());
        review.setReviewerId(adminUser.getId());
        review.setReviewedTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        postReviewMapper.updateById(review);
        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOwnCreateReview(Long reviewId, User currentUser) {
        PostReview review = postReviewMapper.selectById(reviewId);
        if (review == null) {
            return;
        }
        if (!Objects.equals(review.getUserId(), currentUser.getId())) {
            throw new IllegalArgumentException("无权删除该审核记录");
        }
        if ("approved".equals(review.getReviewStatus())) {
            throw new IllegalArgumentException("该记录不能直接删除");
        }
        if (review.getPostId() == null) {
            review.setPostStatus("draft");
        } else {
            Post post = postMapper.selectById(review.getPostId());
            review.setPostStatus(post != null ? post.getStatus() : "draft");
        }
        review.setReviewStatus("withdrawn");
        review.setReviewNote(null);
        review.setReviewerId(null);
        review.setReviewedTime(null);
        review.setUpdateTime(LocalDateTime.now());
        postReviewMapper.updateById(review);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReviewsByPostId(Long postId) {
        postReviewMapper.delete(new QueryWrapper<PostReview>().eq("post_id", postId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteReviewAsAdmin(Long reviewId) {
        if (reviewId == null) {
            return;
        }
        postReviewMapper.deleteById(reviewId);
    }

    private boolean isEditableReview(PostReview review) {
        return "draft".equals(review.getReviewStatus())
                || "withdrawn".equals(review.getReviewStatus())
                || "pending".equals(review.getReviewStatus())
                || "rejected".equals(review.getReviewStatus());
    }

    private int reviewStatusOrder(String status) {
        if ("pending".equals(status)) {
            return 0;
        }
        if ("rejected".equals(status)) {
            return 1;
        }
        if ("approved".equals(status)) {
            return 2;
        }
        return 9;
    }

    private boolean matchesReviewStatus(PostReviewView item, String status) {
        if (item == null) {
            return false;
        }
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return true;
        }
        return status.trim().equalsIgnoreCase(item.getReviewStatus());
    }

    private Map<String, Long> summarizeWorkspace(List<PostWorkspaceItem> items) {
        long pending = items.stream().filter(item -> "pending".equals(item.getReviewStatus())).count();
        long rejected = items.stream().filter(item -> "rejected".equals(item.getReviewStatus())).count();
        long published = items.stream()
                .filter(item -> "post".equals(item.getItemType()) && "published".equals(item.getPostStatus()))
                .count();

        Map<String, Long> summary = new HashMap<>();
        summary.put("total", (long) items.size());
        summary.put("pending", pending);
        summary.put("rejected", rejected);
        summary.put("published", published);
        return summary;
    }

    private Map<String, Long> summarizeReviews(List<PostReviewView> reviews) {
        long pending = reviews.stream().filter(item -> "pending".equals(item.getReviewStatus())).count();
        long approved = reviews.stream().filter(item -> "approved".equals(item.getReviewStatus())).count();
        long rejected = reviews.stream().filter(item -> "rejected".equals(item.getReviewStatus())).count();

        Map<String, Long> summary = new HashMap<>();
        summary.put("total", (long) reviews.size());
        summary.put("pending", pending);
        summary.put("approved", approved);
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

    private void fillReviewSnapshot(PostReview review, Post payload) {
        review.setTitle(payload.getTitle());
        review.setSlug(payload.getSlug());
        review.setContent(payload.getContent());
        review.setSummary(payload.getSummary());
        review.setExcerpt(payload.getExcerpt());
        review.setCategory(payload.getCategory());
        review.setTags(payload.getTags());
        review.setCoverImage(payload.getCoverImage());
        review.setPostStatus(payload.getStatus());
    }

    private void fillPostSnapshot(Post post, PostReview review) {
        post.setTitle(review.getTitle());
        post.setSlug(review.getSlug());
        post.setContent(review.getContent());
        post.setSummary(review.getSummary());
        post.setExcerpt(review.getExcerpt());
        post.setCategory(review.getCategory());
        post.setTags(review.getTags());
        post.setCoverImage(review.getCoverImage());
        post.setStatus(review.getPostStatus());
    }

    private PostWorkspaceItem toWorkspaceItem(PostReview review) {
        PostWorkspaceItem item = new PostWorkspaceItem();
        item.setItemType("review");
        item.setPostId(review.getPostId());
        item.setReviewId(review.getId());
        item.setTitle(review.getTitle());
        item.setSlug(review.getSlug());
        item.setContent(review.getContent());
        item.setSummary(review.getSummary());
        item.setExcerpt(review.getExcerpt());
        item.setCategory(review.getCategory());
        item.setTags(review.getTags());
        item.setCoverImage(review.getCoverImage());
        item.setPostStatus(review.getPostStatus());
        item.setReviewStatus(review.getReviewStatus());
        item.setReviewAction(review.getAction());
        item.setReviewNote(review.getReviewNote());
        item.setSubmitTime(review.getCreateTime());
        item.setReviewedTime(review.getReviewedTime());
        item.setDisplayTime(review.getUpdateTime() != null ? review.getUpdateTime() : review.getCreateTime());
        return item;
    }

    private PostWorkspaceItem toWorkspaceItem(Post post, PostReview review) {
        PostReview attachedReview = shouldAttachReviewToPost(post, review) ? review : null;
        PostWorkspaceItem item = new PostWorkspaceItem();
        item.setItemType("post");
        item.setPostId(post.getId());
        item.setReviewId(attachedReview != null ? attachedReview.getId() : null);
        item.setTitle(attachedReview != null ? attachedReview.getTitle() : post.getTitle());
        item.setSlug(attachedReview != null ? attachedReview.getSlug() : post.getSlug());
        item.setContent(attachedReview != null ? attachedReview.getContent() : post.getContent());
        item.setSummary(attachedReview != null ? attachedReview.getSummary() : post.getSummary());
        item.setExcerpt(attachedReview != null ? attachedReview.getExcerpt() : post.getExcerpt());
        item.setCategory(attachedReview != null ? attachedReview.getCategory() : post.getCategory());
        item.setTags(attachedReview != null ? attachedReview.getTags() : post.getTags());
        item.setCoverImage(attachedReview != null ? attachedReview.getCoverImage() : post.getCoverImage());
        item.setPostStatus(resolveWorkspacePostStatus(post, attachedReview));
        item.setReviewStatus(attachedReview != null ? attachedReview.getReviewStatus() : null);
        item.setReviewAction(attachedReview != null ? attachedReview.getAction() : null);
        item.setReviewNote(attachedReview != null ? attachedReview.getReviewNote() : null);
        item.setSubmitTime(attachedReview != null ? attachedReview.getCreateTime() : post.getCreateTime());
        item.setReviewedTime(attachedReview != null ? attachedReview.getReviewedTime() : null);
        item.setDisplayTime(attachedReview != null && attachedReview.getUpdateTime() != null ? attachedReview.getUpdateTime() : post.getUpdateTime());
        return item;
    }

    private String resolveWorkspacePostStatus(Post post, PostReview review) {
        if (review == null) {
            return post.getStatus();
        }
        if ("pending".equals(review.getReviewStatus())) {
            return review.getPostStatus();
        }
        return post.getStatus();
    }

    private boolean shouldAttachReviewToPost(Post post, PostReview review) {
        if (review == null) {
            return false;
        }
        if ("pending".equals(review.getReviewStatus())) {
            return true;
        }
        if ("withdrawn".equals(review.getReviewStatus())) {
            return false;
        }
        if (!"draft".equals(review.getReviewStatus()) && !"rejected".equals(review.getReviewStatus())) {
            return false;
        }

        LocalDateTime reviewTime = review.getUpdateTime() != null ? review.getUpdateTime() : review.getCreateTime();
        LocalDateTime postTime = post.getUpdateTime() != null ? post.getUpdateTime() : post.getCreateTime();
        if (reviewTime == null || postTime == null) {
            return false;
        }
        return reviewTime.isAfter(postTime);
    }

    private PostReviewView toReviewView(PostReview review, Map<Long, User> userMap) {
        PostReviewView view = new PostReviewView();
        view.setId(review.getId());
        view.setPostId(review.getPostId());
        view.setUserId(review.getUserId());
        User author = userMap.get(review.getUserId());
        if (author != null) {
            view.setUsername(author.getUsername());
            view.setNickname(author.getNickname());
        }
        view.setAction(review.getAction());
        view.setReviewStatus(review.getReviewStatus());
        view.setTitle(review.getTitle());
        view.setSlug(review.getSlug());
        view.setContent(review.getContent());
        view.setSummary(review.getSummary());
        view.setExcerpt(review.getExcerpt());
        view.setCategory(review.getCategory());
        view.setTags(review.getTags());
        view.setCoverImage(review.getCoverImage());
        view.setPostStatus(review.getPostStatus());
        view.setReviewNote(review.getReviewNote());
        view.setReviewerId(review.getReviewerId());
        User reviewer = userMap.get(review.getReviewerId());
        if (reviewer != null) {
            view.setReviewerName(reviewer.getNickname() != null && !reviewer.getNickname().isBlank()
                    ? reviewer.getNickname()
                    : reviewer.getUsername());
        }
        view.setReviewedTime(review.getReviewedTime());
        view.setCreateTime(review.getCreateTime());
        view.setUpdateTime(review.getUpdateTime());
        return view;
    }
}
