package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "Review cannot exceed 2000 characters")
    private String body;
}