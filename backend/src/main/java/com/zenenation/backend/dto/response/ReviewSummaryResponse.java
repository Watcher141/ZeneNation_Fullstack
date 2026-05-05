package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Review summary shown at the top of the reviews section.
 * Contains average rating, total count, and star distribution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryResponse {

    private Double averageRating;       // e.g. 4.3
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution; // {5: 10, 4: 5, 3: 2, 2: 1, 1: 0}
    private Boolean userHasReviewed;    // Has current user already reviewed?
    private Boolean userCanReview;      // Has user purchased and received this product?
}