package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Used for both CREATE and UPDATE category.
 * Image is handled separately via a dedicated image upload API —
 * not part of this DTO (multipart upload is a separate endpoint).
 */
@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
