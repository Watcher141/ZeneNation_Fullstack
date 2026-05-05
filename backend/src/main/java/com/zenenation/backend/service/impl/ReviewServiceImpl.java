package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.request.ReviewRequest;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.ReviewResponse;
import com.zenenation.backend.dto.response.ReviewSummaryResponse;
import com.zenenation.backend.entity.Product;
import com.zenenation.backend.entity.Review;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.OrderRepository;
import com.zenenation.backend.repository.ProductRepository;
import com.zenenation.backend.repository.ReviewRepository;
import com.zenenation.backend.service.ReviewService;
import com.zenenation.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        long total = reviewRepository.countByProductId(productId);

        // Build rating distribution map {5:10, 4:5, 3:2, 2:1, 1:0}
        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0L);

        List<Object[]> raw = reviewRepository.getRatingDistributionByProductId(productId);
        for (Object[] row : raw) {
            distribution.put((Integer) row[0], (Long) row[1]);
        }

        // Check if current user can review / has reviewed
        boolean userHasReviewed = false;
        boolean userCanReview = false;

        try {
            Long userId = securityUtil.getCurrentUserId();
            userHasReviewed = reviewRepository.existsByProductIdAndUserId(productId, userId);
            userCanReview = orderRepository.hasUserPurchasedProduct(userId, productId);
        } catch (Exception ignored) {
            // Not logged in — both stay false
        }

        return ReviewSummaryResponse.builder()
                .averageRating(Math.round(avg * 10.0) / 10.0)
                .totalReviews(total)
                .ratingDistribution(distribution)
                .userHasReviewed(userHasReviewed)
                .userCanReview(userCanReview)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(Long productId, int page, int size) {
        var reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(
                productId, PageRequest.of(page, size)
        );
        return PagedResponse.of(reviews.map(this::toResponse));
    }

    @Override
    @Transactional
    public ReviewResponse submitReview(Long productId, ReviewRequest request) {
        User user = securityUtil.getCurrentUser();

        // Check product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Check already reviewed
        if (reviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        // Check if verified purchaser
        boolean isVerified = orderRepository.hasUserPurchasedProduct(user.getId(), productId);
        if (!isVerified) {
            throw new BadRequestException(
                "You can only review products you have purchased and received"
            );
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .isVerified(true)
                .build();

        review = reviewRepository.save(review);
        log.info("Review submitted: productId={}, userId={}, rating={}", productId, user.getId(), request.getRating());
        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long productId, ReviewRequest request) {
        User user = securityUtil.getCurrentUser();

        Review review = reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for this product"));

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setBody(request.getBody());

        review = reviewRepository.save(review);
        log.info("Review updated: productId={}, userId={}", productId, user.getId());
        return toResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long productId) {
        User user = securityUtil.getCurrentUser();
        Review review = reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        reviewRepository.delete(review);
        log.info("Review deleted by user: productId={}, userId={}", productId, user.getId());
    }

    @Override
    @Transactional
    public void adminDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
        reviewRepository.delete(review);
        log.info("Review deleted by admin: reviewId={}", reviewId);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getName())
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .isVerified(review.getIsVerified())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}