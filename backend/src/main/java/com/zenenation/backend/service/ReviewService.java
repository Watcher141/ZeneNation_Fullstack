package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.ReviewRequest;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.ReviewResponse;
import com.zenenation.backend.dto.response.ReviewSummaryResponse;

public interface ReviewService {

    /** Get review summary stats for a product */
    ReviewSummaryResponse getReviewSummary(Long productId);

    /** Get paginated reviews for a product */
    PagedResponse<ReviewResponse> getProductReviews(Long productId, int page, int size);

    /** Submit a new review — only verified purchasers */
    ReviewResponse submitReview(Long productId, ReviewRequest request);

    /** Update own review */
    ReviewResponse updateReview(Long productId, ReviewRequest request);

    /** Delete own review */
    void deleteReview(Long productId);

    /** Admin: delete any review */
    void adminDeleteReview(Long reviewId);
}