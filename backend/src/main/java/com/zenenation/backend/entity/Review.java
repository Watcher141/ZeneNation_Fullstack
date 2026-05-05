package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A product review submitted by a user.
 *
 * Rules enforced:
 * - One review per user per product (unique constraint in DB)
 * - Only verified purchasers can review (checked in service)
 * - Rating must be 1-5 (checked in DB + validation)
 */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_reviews_user_product",
            columnNames = {"user_id", "product_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating;          // 1 to 5

    @Column(length = 150)
    private String title;            // Optional short title

    @Column(columnDefinition = "TEXT")
    private String body;             // Optional detailed review text

    /**
     * True if the user has a DELIVERED order containing this product.
     * Verified reviews are shown with a "Verified Purchase" badge.
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}