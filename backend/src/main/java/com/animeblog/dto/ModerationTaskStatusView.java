package com.animeblog.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ModerationTaskStatusView {

    private String taskId;

    private String targetType;

    private String operation;

    private String status;

    private int totalCount;

    private int processedCount;

    private int successCount;

    private int failureCount;

    private String message;

    private List<String> errors = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
