package com.animeblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentReportCreateRequest {

    @NotBlank(message = "Reason code cannot be empty")
    @Size(max = 32, message = "Reason code must be 32 characters or fewer")
    private String reasonCode;

    @Size(max = 100, message = "Custom reason must be 100 characters or fewer")
    private String otherReason;

    @NotBlank(message = "Report description cannot be empty")
    @Size(max = 1000, message = "Report description must be 1000 characters or fewer")
    private String description;
}
