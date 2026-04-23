package com.animeblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentReviewDecisionRequest {

    @NotBlank(message = "Review note cannot be empty")
    @Size(max = 500, message = "Review note must be 500 characters or fewer")
    private String reviewNote;
}
