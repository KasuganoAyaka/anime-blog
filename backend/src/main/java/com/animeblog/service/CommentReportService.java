package com.animeblog.service;

import com.animeblog.dto.AdminPageResult;
import com.animeblog.dto.CommentReportCreateRequest;
import com.animeblog.dto.CommentReportView;
import com.animeblog.entity.CommentReport;
import com.animeblog.entity.Post;
import com.animeblog.entity.PostComment;
import com.animeblog.entity.User;
import com.animeblog.exception.BusinessException;
import com.animeblog.mapper.CommentReportMapper;
import com.animeblog.mapper.PostCommentMapper;
import com.animeblog.mapper.PostMapper;
import com.animeblog.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
public class CommentReportService {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_RESOLVED = "resolved";
    public static final String ACTION_KEPT = "kept";
    public static final String ACTION_DELETED = "deleted";

    private static final Map<String, String> REASON_LABELS = Map.of(
            "spam", "垃圾信息",
            "abuse", "辱骂攻击",
            "ads", "广告引流",
            "illegal", "违法违规",
            "porn", "色情低俗",
            "privacy", "隐私泄露",
            "other", "其他"
    );

    @Autowired
    private CommentReportMapper commentReportMapper;

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostCommentService postCommentService;

    public void createReport(Long postId, Long commentId, CommentReportCreateRequest request, User currentUser) {
        PostComment comment = requireVisibleComment(postId, commentId);
        String reasonCode = normalizeReasonCode(request.getReasonCode());
        String otherReason = trimToNull(request.getOtherReason(), 100);
        String description = trimToNull(request.getDescription(), 1000);

        if ("other".equals(reasonCode) && !StringUtils.hasText(otherReason)) {
            throw new BusinessException("请选择其他原因时请补充说明");
        }
        if (!StringUtils.hasText(description)) {
            throw new BusinessException("请填写举报说明");
        }

        LocalDateTime now = LocalDateTime.now();
        CommentReport report = new CommentReport();
        report.setCommentId(comment.getId());
        report.setPostId(comment.getPostId());
        report.setReporterId(currentUser == null ? null : currentUser.getId());
        report.setReasonCode(reasonCode);
        report.setReasonLabel(REASON_LABELS.get(reasonCode));
        report.setOtherReason(otherReason);
        report.setDescription(description);
        report.setStatus(STATUS_PENDING);
        report.setResolutionAction(null);
        report.setResolutionNote(null);
        report.setResolvedBy(null);
        report.setResolvedTime(null);
        report.setCommentAuthorName(trimToNull(comment.getAuthorName(), 100));
        report.setCommentContent(trimToNull(comment.getContent(), 65535));
        report.setCreateTime(now);
        report.setUpdateTime(now);
        commentReportMapper.insert(report);
    }

    public List<CommentReportView> getReportsForAdmin() {
        return buildAdminReports("all", null);
    }

    public AdminPageResult<CommentReportView> getReportsForAdmin(String status, String keyword, int page, int size) {
        List<CommentReportView> scopedReports = buildAdminReports("all", keyword);
        List<CommentReportView> filteredReports = scopedReports.stream()
                .filter(item -> matchesReportStatus(item, status))
                .toList();
        return paginate(filteredReports, page, size, summarizeReports(scopedReports));
    }

    private List<CommentReportView> buildAdminReports(String status, String keyword) {
        QueryWrapper<CommentReport> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
            wrapper.eq("status", status.trim());
        }
        wrapper.orderByDesc("create_time", "id");
        List<CommentReport> reports = commentReportMapper.selectList(wrapper);
        if (reports.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> postIds = reports.stream()
                .map(CommentReport::getPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Post> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : postMapper.selectBatchIds(postIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(Post::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Set<Long> commentIds = reports.stream()
                .map(CommentReport::getCommentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, PostComment> commentMap = commentIds.isEmpty()
                ? Collections.emptyMap()
                : postCommentMapper.selectBatchIds(commentIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(PostComment::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Set<Long> parentIds = commentMap.values().stream()
                .map(PostComment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, PostComment> parentMap = parentIds.isEmpty()
                ? Collections.emptyMap()
                : postCommentMapper.selectBatchIds(parentIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(PostComment::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Set<Long> userIds = new LinkedHashSet<>();
        for (CommentReport report : reports) {
            if (report.getReporterId() != null) {
                userIds.add(report.getReporterId());
            }
            if (report.getResolvedBy() != null) {
                userIds.add(report.getResolvedBy());
            }
        }
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<CommentReportView> result = new ArrayList<>(reports.size());
        for (CommentReport report : reports) {
            result.add(toView(report, postMap, commentMap, parentMap, userMap));
        }

        result.sort(Comparator
                .comparing((CommentReportView item) -> STATUS_PENDING.equals(item.getStatus()) ? 0 : 1)
                .thenComparing(CommentReportView::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CommentReportView::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return result.stream()
                .filter(item -> matchesReportKeyword(item, keyword))
                .toList();
    }

    public CommentReportView resolveReport(Long reportId, User adminUser, String action, String resolutionNote) {
        CommentReport report = requireReport(reportId);
        if (STATUS_RESOLVED.equals(report.getStatus())) {
            return getSingleReport(reportId);
        }

        String normalizedAction = normalizeResolutionAction(action);
        String normalizedNote = trimToNull(resolutionNote, 500);

        if (ACTION_DELETED.equals(normalizedAction)) {
            Set<Long> deletedIds = postCommentService.deleteCommentAndReturnIds(report.getCommentId());
            if (deletedIds.isEmpty() && report.getCommentId() != null) {
                deletedIds = Set.of(report.getCommentId());
            }
            resolvePendingReports(deletedIds, normalizedAction, adminUser, normalizedNote);
        } else {
            resolvePendingReports(Set.of(report.getCommentId()), normalizedAction, adminUser, normalizedNote);
        }

        return getSingleReport(reportId);
    }

    public void resolveReportsForDeletedComments(Collection<Long> commentIds, User adminUser, String resolutionNote) {
        resolvePendingReports(commentIds, ACTION_DELETED, adminUser, trimToNull(resolutionNote, 500));
    }

    private void resolvePendingReports(Collection<Long> commentIds, String action, User adminUser, String resolutionNote) {
        if (commentIds == null || commentIds.isEmpty()) {
            return;
        }

        QueryWrapper<CommentReport> wrapper = new QueryWrapper<>();
        wrapper.in("comment_id", commentIds).eq("status", STATUS_PENDING);
        List<CommentReport> reports = commentReportMapper.selectList(wrapper);
        if (reports.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Long adminUserId = adminUser == null ? null : adminUser.getId();
        for (CommentReport report : reports) {
            report.setStatus(STATUS_RESOLVED);
            report.setResolutionAction(action);
            report.setResolutionNote(resolutionNote);
            report.setResolvedBy(adminUserId);
            report.setResolvedTime(now);
            report.setUpdateTime(now);
            commentReportMapper.updateById(report);
        }
    }

    private CommentReportView getSingleReport(Long reportId) {
        return buildAdminReports("all", null).stream()
                .filter(item -> Objects.equals(item.getId(), reportId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("举报记录不存在"));
    }

    private CommentReport requireReport(Long reportId) {
        if (reportId == null) {
            throw new BusinessException("举报记录不存在");
        }
        CommentReport report = commentReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        return report;
    }

    private PostComment requireVisibleComment(Long postId, Long commentId) {
        QueryWrapper<PostComment> wrapper = new QueryWrapper<>();
        wrapper.eq("id", commentId)
                .eq("post_id", postId)
                .eq("status", "visible")
                .last("LIMIT 1");
        PostComment comment = postCommentMapper.selectOne(wrapper);
        if (comment == null) {
            throw new BusinessException("要举报的评论不存在或已不可见");
        }
        return comment;
    }

    private CommentReportView toView(
            CommentReport report,
            Map<Long, Post> postMap,
            Map<Long, PostComment> commentMap,
            Map<Long, PostComment> parentMap,
            Map<Long, User> userMap
    ) {
        CommentReportView view = new CommentReportView();
        PostComment comment = report.getCommentId() == null ? null : commentMap.get(report.getCommentId());
        PostComment parent = comment != null && comment.getParentId() != null ? parentMap.get(comment.getParentId()) : null;
        User reporter = report.getReporterId() == null ? null : userMap.get(report.getReporterId());
        User resolver = report.getResolvedBy() == null ? null : userMap.get(report.getResolvedBy());
        Post post = report.getPostId() == null ? null : postMap.get(report.getPostId());

        view.setId(report.getId());
        view.setCommentId(report.getCommentId());
        view.setPostId(report.getPostId());
        view.setPostTitle(post == null ? "" : defaultString(post.getTitle()));
        view.setParentId(comment == null ? null : comment.getParentId());
        view.setParentAuthorName(parent == null ? "" : defaultString(parent.getAuthorName()));
        view.setCommentAuthorName(comment == null ? defaultString(report.getCommentAuthorName()) : defaultString(comment.getAuthorName()));
        view.setCommentContent(comment == null ? defaultString(report.getCommentContent()) : defaultString(comment.getContent()));
        view.setCommentStatus(comment == null ? ACTION_DELETED : defaultString(comment.getStatus()));
        view.setCommentDeleted(comment == null);
        view.setReporterId(report.getReporterId());
        view.setReporterName(resolveUserDisplayName(reporter, "访客"));
        view.setReporterRole(reporter == null ? "" : defaultString(reporter.getRole()));
        view.setReasonCode(defaultString(report.getReasonCode()));
        view.setReasonLabel(defaultString(report.getReasonLabel()));
        view.setOtherReason(defaultString(report.getOtherReason()));
        view.setDescription(defaultString(report.getDescription()));
        view.setStatus(defaultString(report.getStatus()));
        view.setResolutionAction(defaultString(report.getResolutionAction()));
        view.setResolutionNote(defaultString(report.getResolutionNote()));
        view.setResolvedBy(report.getResolvedBy());
        view.setResolverName(resolveUserDisplayName(resolver, ""));
        view.setResolvedAt(report.getResolvedTime());
        view.setCreatedAt(report.getCreateTime());
        return view;
    }

    private String normalizeReasonCode(String reasonCode) {
        String normalized = trimToNull(reasonCode, 32);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("举报原因不能为空");
        }
        String result = normalized.toLowerCase(Locale.ROOT);
        if (!REASON_LABELS.containsKey(result)) {
            throw new BusinessException("不支持的举报原因");
        }
        return result;
    }

    private String normalizeResolutionAction(String action) {
        String normalized = trimToNull(action, 20);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("处理动作不能为空");
        }

        String result = normalized.toLowerCase(Locale.ROOT);
        if (!ACTION_KEPT.equals(result) && !ACTION_DELETED.equals(result)) {
            throw new BusinessException("不支持的处理动作");
        }
        return result;
    }

    private String resolveUserDisplayName(User user, String fallback) {
        if (user == null) {
            return fallback;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return fallback;
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

    private boolean matchesReportStatus(CommentReportView item, String status) {
        if (item == null) {
            return false;
        }
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return true;
        }
        return status.trim().equalsIgnoreCase(item.getStatus());
    }

    private boolean matchesReportKeyword(CommentReportView item, String keyword) {
        String normalizedKeyword = trimToNull(keyword, 100);
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }

        String searchableText = String.join("\n",
                defaultString(item.getPostTitle()),
                defaultString(item.getReasonLabel()),
                defaultString(item.getOtherReason()),
                defaultString(item.getDescription()),
                defaultString(item.getCommentAuthorName()),
                defaultString(item.getCommentContent()),
                defaultString(item.getReporterName())
        ).toLowerCase(Locale.ROOT);

        return searchableText.contains(normalizedKeyword.toLowerCase(Locale.ROOT));
    }

    private Map<String, Long> summarizeReports(List<CommentReportView> items) {
        long pending = items.stream().filter(item -> STATUS_PENDING.equals(item.getStatus())).count();
        long resolved = items.stream().filter(item -> STATUS_RESOLVED.equals(item.getStatus())).count();

        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("total", (long) items.size());
        summary.put("pending", pending);
        summary.put("resolved", resolved);
        return summary;
    }

    private <T> AdminPageResult<T> paginate(List<T> items, int page, int size, Map<String, Long> summary) {
        long current = Math.max(1L, page);
        long pageSize = Math.max(1L, size);
        int fromIndex = (int) Math.min((current - 1L) * pageSize, items.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, items.size());
        return AdminPageResult.of(items.subList(fromIndex, toIndex), current, pageSize, items.size(), summary);
    }
}
