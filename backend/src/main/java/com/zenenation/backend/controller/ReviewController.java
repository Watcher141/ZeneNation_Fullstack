package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.ReviewRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.ReviewResponse;
import com.zenenation.backend.dto.response.ReviewSummaryResponse;
import com.zenenation.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** GET /api/v1/reviews/product/{productId}/summary */
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getSummary(
            @PathVariable Long productId) {
        return ResponseEntity.ok(
                ApiResponse.success("Review summary fetched", reviewService.getReviewSummary(productId)));
    }

    /** GET /api/v1/reviews/product/{productId}?page=0&size=10 */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Reviews fetched", reviewService.getProductReviews(productId, page, size)));
    }

    /** POST /api/v1/reviews/product/{productId} */
    @PostMapping("/product/{productId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully",
                        reviewService.submitReview(productId, request)));
    }

    /** PUT /api/v1/reviews/product/{productId} */
    @PutMapping("/product/{productId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Review updated successfully",
                        reviewService.updateReview(productId, request)));
    }

    /** DELETE /api/v1/reviews/product/{productId} */
    @DeleteMapping("/product/{productId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long productId) {
        reviewService.deleteReview(productId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted"));
    }

    /** DELETE /api/v1/reviews/{reviewId}/admin */
    @DeleteMapping("/{reviewId}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> adminDeleteReview(
            @PathVariable Long reviewId) {
        reviewService.adminDeleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted by admin"));
    }
}