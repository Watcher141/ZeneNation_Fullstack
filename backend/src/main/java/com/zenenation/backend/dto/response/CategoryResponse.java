package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Returned for both public and admin category endpoints.
 * Never exposes imagePublicId to the frontend —
 * that's an internal Cloudinary key, not needed by the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private Boolean isDeleted;
    private Long parentId;                    // null for top-level categories
    private String parentName;               // name of parent category
    private List<CategoryResponse> subcategories;  // populated for top-level
    private Integer productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}