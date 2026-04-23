package com.animeblog.service;

import com.animeblog.dto.BatchModerationRequest;
import com.animeblog.dto.ModerationTaskStatusView;
import com.animeblog.entity.User;
import com.animeblog.exception.BusinessException;
import com.animeblog.util.ExpiringCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class ModerationTaskService {

    private static final String STATUS_QUEUED = "queued";
    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";

    private static final String TARGET_POST_REVIEW = "post-review";
    private static final String TARGET_COMMENT = "comment";

    private static final long TASK_TTL_MILLIS = 2 * 60 * 60 * 1000L;

    private final ExpiringCache<ModerationTaskStatusView> taskCache = new ExpiringCache<>(600);

    @Autowired
    @Qualifier("moderationTaskExecutor")
    private Executor moderationTaskExecutor;

    @Autowired
    private PostReviewService postReviewService;

    @Autowired
    private PostCommentService postCommentService;

    @Autowired
    private CommentReportService commentReportService;

    public ModerationTaskStatusView submitReviewBatch(String operation, BatchModerationRequest request, User adminUser) {
        List<Long> ids = normalizeIds(request);
        String normalizedOperation = normalizeOperation(operation);
        String normalizedNote = trimToNull(request == null ? null : request.getReviewNote(), 500);
        if ("reject".equals(normalizedOperation) && !StringUtils.hasText(normalizedNote)) {
            throw new BusinessException("批量驳回时请填写驳回原因");
        }

        ModerationTaskStatusView task = createTask(TARGET_POST_REVIEW, normalizedOperation, ids.size());
        moderationTaskExecutor.execute(() -> runReviewTask(task, ids, normalizedNote, adminUser));
        return task;
    }

    public ModerationTaskStatusView submitCommentBatch(String operation, BatchModerationRequest request, User adminUser) {
        List<Long> ids = normalizeIds(request);
        String normalizedOperation = normalizeOperation(operation);
        String normalizedNote = trimToNull(request == null ? null : request.getReviewNote(), 500);
        if ("reject".equals(normalizedOperation) && !StringUtils.hasText(normalizedNote)) {
            throw new BusinessException("批量驳回时请填写驳回原因");
        }

        ModerationTaskStatusView task = createTask(TARGET_COMMENT, normalizedOperation, ids.size());
        moderationTaskExecutor.execute(() -> runCommentTask(task, ids, normalizedNote, adminUser));
        return task;
    }

    public ModerationTaskStatusView getTask(String taskId) {
        ModerationTaskStatusView task = taskCache.get(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在或已过期");
        }
        return task;
    }

    private void runReviewTask(ModerationTaskStatusView task, List<Long> ids, String reviewNote, User adminUser) {
        markRunning(task);
        for (Long id : ids) {
            try {
                switch (task.getOperation()) {
                    case "approve" -> postReviewService.approveReview(id, adminUser);
                    case "reject" -> postReviewService.rejectReview(id, adminUser, reviewNote);
                    case "delete" -> postReviewService.deleteReviewAsAdmin(id);
                    default -> throw new BusinessException("不支持的批量操作");
                }
                markSuccess(task);
            } catch (Exception exception) {
                markFailure(task, buildErrorMessage(id, exception));
            }
        }
        finishTask(task);
    }

    private void runCommentTask(ModerationTaskStatusView task, List<Long> ids, String reviewNote, User adminUser) {
        markRunning(task);
        for (Long id : ids) {
            try {
                switch (task.getOperation()) {
                    case "approve" -> postCommentService.approveComment(id, adminUser);
                    case "reject" -> postCommentService.rejectComment(id, adminUser, reviewNote);
                    case "delete" -> {
                        Set<Long> deletedIds = postCommentService.deleteCommentAndReturnIds(id);
                        if (!deletedIds.isEmpty()) {
                            commentReportService.resolveReportsForDeletedComments(deletedIds, adminUser, reviewNote);
                        }
                    }
                    default -> throw new BusinessException("不支持的批量操作");
                }
                markSuccess(task);
            } catch (Exception exception) {
                markFailure(task, buildErrorMessage(id, exception));
            }
        }
        finishTask(task);
    }

    private ModerationTaskStatusView createTask(String targetType, String operation, int totalCount) {
        ModerationTaskStatusView task = new ModerationTaskStatusView();
        task.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        task.setTargetType(targetType);
        task.setOperation(operation);
        task.setStatus(STATUS_QUEUED);
        task.setTotalCount(totalCount);
        task.setProcessedCount(0);
        task.setSuccessCount(0);
        task.setFailureCount(0);
        task.setMessage("任务已创建");
        task.setCreatedAt(LocalDateTime.now());
        taskCache.put(task.getTaskId(), task, TASK_TTL_MILLIS);
        return task;
    }

    private void markRunning(ModerationTaskStatusView task) {
        task.setStatus(STATUS_RUNNING);
        task.setStartedAt(LocalDateTime.now());
        task.setMessage("任务执行中");
    }

    private void markSuccess(ModerationTaskStatusView task) {
        task.setProcessedCount(task.getProcessedCount() + 1);
        task.setSuccessCount(task.getSuccessCount() + 1);
    }

    private void markFailure(ModerationTaskStatusView task, String errorMessage) {
        task.setProcessedCount(task.getProcessedCount() + 1);
        task.setFailureCount(task.getFailureCount() + 1);
        List<String> errors = task.getErrors();
        if (errors.size() < 20) {
            errors.add(errorMessage);
        }
    }

    private void finishTask(ModerationTaskStatusView task) {
        task.setCompletedAt(LocalDateTime.now());
        if (task.getFailureCount() > 0 && task.getSuccessCount() == 0) {
            task.setStatus(STATUS_FAILED);
        } else {
            task.setStatus(STATUS_COMPLETED);
        }
        task.setMessage(String.format(
                Locale.ROOT,
                "处理完成：成功 %d 条，失败 %d 条，共 %d 条",
                task.getSuccessCount(),
                task.getFailureCount(),
                task.getTotalCount()
        ));
    }

    private List<Long> normalizeIds(BatchModerationRequest request) {
        if (request == null || request.getIds() == null || request.getIds().isEmpty()) {
            throw new BusinessException("请选择要处理的记录");
        }

        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long id : request.getIds()) {
            if (id != null) {
                uniqueIds.add(id);
            }
        }
        if (uniqueIds.isEmpty()) {
            throw new BusinessException("请选择要处理的记录");
        }
        return new ArrayList<>(uniqueIds);
    }

    private String normalizeOperation(String operation) {
        String normalized = trimToNull(operation, 20);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("批量操作不能为空");
        }
        String result = normalized.toLowerCase(Locale.ROOT);
        if (!"approve".equals(result) && !"reject".equals(result) && !"delete".equals(result)) {
            throw new BusinessException("不支持的批量操作");
        }
        return result;
    }

    private String buildErrorMessage(Long id, Exception exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            message = "未知错误";
        }
        return "ID " + id + "：" + message;
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
}
