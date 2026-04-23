package com.animeblog.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchModerationRequest {

    @NotEmpty(message = "Ids cannot be empty")
    @Size(max = 200, message = "Batch size cannot exceed 200 items")
    private List<Long> ids;

    @Size(max = 500, message = "Review note must be 500 characters or fewer")
    private String reviewNote;
}
