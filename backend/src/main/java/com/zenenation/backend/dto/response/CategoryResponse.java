package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Boolean isDeleted;      // Only relevant for admin view
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
