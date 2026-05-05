package com.zenenation.backend.repository;

import com.zenenation.backend.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /**
     * Count how many times a specific user has used a specific coupon.
     * Used to enforce per-user usage limit.
     */
    int countByCouponIdAndUserId(Long couponId, Long userId);

    /**
     * Check if a coupon was already used on a specific order.
     * Prevents applying two coupons to the same order.
     */
    boolean existsByCouponIdAndOrderId(Long couponId, Long orderId);
}
