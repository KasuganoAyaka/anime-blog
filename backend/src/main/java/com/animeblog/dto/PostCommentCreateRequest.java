package com.animeblog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PostCommentCreateRequest {

    private Long parentId;

    @Size(max = 100, message = "Comment nickname must be 100 characters or fewer")
    private String authorName;

    @Email(message = "Comment email format is invalid")
    @Size(max = 100, message = "Comment email must be 100 characters or fewer")
    private String authorEmail;

    @Size(max = 255, message = "Comment website must be 255 characters or fewer")
    private String authorWebsite;

    @Size(max = 100000, message = "Comment content must be 100000 characters or fewer")
    private String content;

    @Size(max = 9, message = "A comment can contain at most 9 images")
    private List<@Size(max = 500, message = "Image URL must be 500 characters or fewer") String> images;

    private Boolean guestMode;

    private Boolean anonymous;

    private String turnstileToken;
}
