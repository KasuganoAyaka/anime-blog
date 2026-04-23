package com.animeblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentReportResolutionRequest {

    @NotBlank(message = "Resolution action cannot be empty")
    @Size(max = 20, message = "Resolution action must be 20 characters or fewer")
    private String action;

    @Size(max = 500, message = "Resolution note must be 500 characters or fewer")
    private String resolutionNote;
}
