package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.ApplyCouponRequest;
import com.zenenation.backend.dto.request.CouponRequest;
import com.zenenation.backend.dto.response.CouponResponse;
import com.zenenation.backend.dto.response.CouponValidationResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.entity.Coupon;

import java.math.BigDecimal;

public interface CouponService {

    // ── Admin ──────────────────────────────────────────────────────────────

    /** Admin: get all coupons paginated */
    PagedResponse<CouponResponse> getAllCoupons(int page, int size);

    /** Admin: get single coupon by ID */
    CouponResponse getCouponById(Long id);

    /** Admin: create a new coupon */
    CouponResponse createCoupon(CouponRequest request);

    /** Admin: update an existing coupon */
    CouponResponse updateCoupon(Long id, CouponRequest request);

    /** Admin: toggle coupon active/inactive */
    CouponResponse toggleCoupon(Long id);

    /** Admin: delete a coupon */
    void deleteCoupon(Long id);

    // ── User ───────────────────────────────────────────────────────────────

    /**
     * Validate a coupon code against current cart total.
     * Returns discount preview — does NOT apply the coupon yet.
     * Called when user types a code at checkout to show savings.
     */
    CouponValidationResponse validateCoupon(ApplyCouponRequest request, BigDecimal cartTotal);

    // ── Internal (called from OrderService) ───────────────────────────────

    /**
     * Validate AND return the Coupon entity for use during order placement.
     * Throws BadRequestException if coupon is invalid.
     */
    Coupon validateAndGetCoupon(String code, BigDecimal cartTotal, Long userId);

    /**
     * Calculate the actual discount amount for a given coupon and subtotal.
     * Respects maximumDiscount cap for PERCENTAGE coupons.
     */
    BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal);

    /**
     * Record coupon usage after successful order placement.
     * Increments usedCount and creates CouponUsage record.
     */
    void recordCouponUsage(Coupon coupon, Long userId, Long orderId);
}
