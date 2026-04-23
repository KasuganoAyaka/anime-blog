package com.animeblog.controller;

import com.animeblog.dto.ApiResponse;
import com.animeblog.dto.BatchModerationRequest;
import com.animeblog.dto.CommentReportResolutionRequest;
import com.animeblog.dto.CommentReviewDecisionRequest;
import com.animeblog.entity.User;
import com.animeblog.exception.ForbiddenException;
import com.animeblog.service.CommentReportService;
import com.animeblog.service.ModerationTaskService;
import com.animeblog.service.PostCommentService;
import com.animeblog.util.AuthHelper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private static final String ADMIN_FORBIDDEN_MESSAGE = "无权限访问管理员接口";

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private PostCommentService postCommentService;

    @Autowired
    private ModerationTaskService moderationTaskService;

    @Autowired
    private CommentReportService commentReportService;

    @GetMapping
    public ApiResponse<?> getComments(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(postCommentService.getCommentsForAdmin(status, keyword, postId, page, size));
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<?> approveComment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(postCommentService.approveComment(id, currentUser));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<?> rejectComment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody CommentReviewDecisionRequest request
    ) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(postCommentService.rejectComment(id, currentUser, request.getReviewNote()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteComment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        Set<Long> deletedIds = postCommentService.deleteCommentAndReturnIds(id);
        if (!deletedIds.isEmpty()) {
            commentReportService.resolveReportsForDeletedComments(deletedIds, currentUser, "评论已由管理员删除");
        }
        return ApiResponse.success();
    }

    @PostMapping("/batch/{operation}")
    public ApiResponse<?> submitBatchTask(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String operation,
            @Valid @RequestBody BatchModerationRequest request
    ) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(moderationTaskService.submitCommentBatch(operation, request, currentUser));
    }

    @GetMapping("/reports")
    public ApiResponse<?> getCommentReports(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(commentReportService.getReportsForAdmin(status, keyword, page, size));
    }

    @PutMapping("/reports/{id}/resolve")
    public ApiResponse<?> resolveCommentReport(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody CommentReportResolutionRequest request
    ) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(commentReportService.resolveReport(
                id,
                currentUser,
                request.getAction(),
                request.getResolutionNote()
        ));
    }
}
