package com.animeblog.controller;

import com.animeblog.dto.ApiResponse;
import com.animeblog.entity.User;
import com.animeblog.exception.ForbiddenException;
import com.animeblog.service.ModerationTaskService;
import com.animeblog.util.AuthHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/moderation-tasks")
public class AdminModerationTaskController {

    private static final String ADMIN_FORBIDDEN_MESSAGE = "无权限访问管理员接口";

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private ModerationTaskService moderationTaskService;

    @GetMapping("/{taskId}")
    public ApiResponse<?> getTaskStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String taskId
    ) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(moderationTaskService.getTask(taskId));
    }
}
