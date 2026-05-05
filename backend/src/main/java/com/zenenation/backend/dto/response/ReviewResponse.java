package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A single review — shown in review list under a product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long productId;
    private Long userId;
    private String userName;            // Reviewer's name
    private Integer rating;             // 1–5
    private String title;
    private String body;
    private Boolean isVerified;         // Verified purchase badge
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}