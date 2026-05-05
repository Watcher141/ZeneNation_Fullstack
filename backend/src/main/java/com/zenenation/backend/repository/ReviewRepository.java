package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** All reviews for a product — paginated, newest first */
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /** Check if user already reviewed this product */
    boolean existsByProductIdAndUserId(Long productId, Long userId);

    /** Get a user's review for a specific product */
    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);

    /** Average rating for a product */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.product.id = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    /** Total review count for a product */
    long countByProductId(Long productId);

    /** Rating distribution — count per star (1-5) */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId GROUP BY r.rating ORDER BY r.rating DESC")
    java.util.List<Object[]> getRatingDistributionByProductId(@Param("productId") Long productId);

    /** All reviews by a user */
    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}