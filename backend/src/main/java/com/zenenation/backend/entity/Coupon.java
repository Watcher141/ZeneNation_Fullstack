package com.zenenation.backend.entity;

import com.zenenation.backend.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A discount coupon created by admin.
 *
 * Supports two types:
 * PERCENTAGE → 20% off (with optional max cap)
 * FLAT       → ₹100 off
 *
 * Admin controls:
 * - Usage limit (total uses allowed)
 * - Per-user limit (how many times one user can use it)
 * - Minimum order amount to qualify
 * - Valid date range
 * - Active/inactive toggle
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;                    // e.g. SAVE20, LAUNCH50, FLAT100

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "minimum_order_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    /**
     * For PERCENTAGE coupons — cap the maximum discount.
     * Example: 20% off but max ₹500.
     * NULL = no cap.
     */
    @Column(name = "maximum_discount", precision = 10, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;             // NULL = unlimited

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "per_user_limit", nullable = false)
    @Builder.Default
    private Integer perUserLimit = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * When set, ONLY this user can use the coupon.
     * Used for welcome coupons auto-generated on registration.
     * NULL = any user can use it (standard admin coupons).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    /**
     * True for system-generated welcome coupons.
     * These are hidden from admin coupon list.
     */
    @Column(name = "is_welcome_coupon", nullable = false)
    @Builder.Default
    private Boolean isWelcomeCoupon = false;

    @Column(name = "valid_from", nullable = false)
    @Builder.Default
    private LocalDateTime validFrom = LocalDateTime.now();

    @Column(name = "valid_until")
    private LocalDateTime validUntil;       // NULL = no expiry

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if coupon is currently valid (active + within date range + has uses left).
     */
    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();
        boolean withinDateRange = !now.isBefore(validFrom)
                && (validUntil == null || now.isBefore(validUntil));
        boolean hasUsesLeft = usageLimit == null || usedCount < usageLimit;
        return isActive && withinDateRange && hasUsesLeft;
    }
}