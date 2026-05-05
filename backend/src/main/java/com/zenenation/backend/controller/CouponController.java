package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.ApplyCouponRequest;
import com.zenenation.backend.dto.request.CouponRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.CouponResponse;
import com.zenenation.backend.dto.response.CouponValidationResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.service.CartService;
import com.zenenation.backend.service.CouponService;
import com.zenenation.backend.repository.CouponRepository;
import com.zenenation.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Coupons", description = "Coupon management (admin) and validation (user)")
public class CouponController {

    private final CouponService couponService;
    private final CouponRepository couponRepository;
    private final SecurityUtil securityUtil;

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/coupons/admin
     * All coupons with full details + usage stats.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: get all coupons")
    public ResponseEntity<ApiResponse<PagedResponse<CouponResponse>>> getAllCoupons(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("Coupons fetched",
                couponService.getAllCoupons(page, size)));
    }

    /**
     * GET /api/v1/coupons/admin/{id}
     * Single coupon detail.
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: get coupon by ID")
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Coupon fetched",
                couponService.getCouponById(id)));
    }

    /**
     * POST /api/v1/coupons/admin
     * Create a new coupon.
     *
     * Example body:
     * {
     *   "code": "SAVE20",
     *   "discountType": "PERCENTAGE",
     *   "discountValue": 20,
     *   "minimumOrderAmount": 500,
     *   "maximumDiscount": 300,
     *   "usageLimit": 100,
     *   "perUserLimit": 1,
     *   "validUntil": "2024-12-31T23:59:59"
     * }
     */
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: create coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(
            @Valid @RequestBody CouponRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created successfully",
                        couponService.createCoupon(request)));
    }

    /**
     * PUT /api/v1/coupons/admin/{id}
     * Update an existing coupon.
     */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: update coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Coupon updated",
                couponService.updateCoupon(id, request)));
    }

    /**
     * PATCH /api/v1/coupons/admin/{id}/toggle
     * Activate or deactivate a coupon instantly.
     */
    @PatchMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: toggle coupon active/inactive")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleCoupon(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Coupon status updated",
                couponService.toggleCoupon(id)));
    }

    /**
     * DELETE /api/v1/coupons/admin/{id}
     * Permanently delete a coupon.
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: delete coupon")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted"));
    }

    // ── USER ──────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/coupons/validate?cartTotal=1000
     * Validate coupon against current cart total — shows discount preview.
     * Does NOT apply the coupon — just shows what savings the user will get.
     * Frontend calls this when user types a coupon code.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate coupon and preview discount")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validateCoupon(
            @Valid @RequestBody ApplyCouponRequest request,
            @RequestParam BigDecimal cartTotal) {

        return ResponseEntity.ok(ApiResponse.success("Coupon is valid",
                couponService.validateCoupon(request, cartTotal)));
    }

    /** GET /api/v1/coupons/my-welcome — get current user's welcome coupon */
    @GetMapping("/my-welcome")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.zenenation.backend.dto.response.CouponResponse>> getMyWelcomeCoupon() {
        Long userId = securityUtil.getCurrentUserId();
        return couponRepository.findWelcomeCouponByUserId(userId)
                .map(c -> ResponseEntity.ok(ApiResponse.success("Welcome coupon found",
                        com.zenenation.backend.dto.response.CouponResponse.builder()
                                .id(c.getId())
                                .code(c.getCode())
                                .description(c.getDescription())
                                .discountType(c.getDiscountType())
                                .discountValue(c.getDiscountValue())
                                .maximumDiscount(c.getMaximumDiscount())
                                .usageLimit(c.getUsageLimit())
                                .usedCount(c.getUsedCount())
                                .isActive(c.getIsActive())
                                .validUntil(c.getValidUntil())
                                .isCurrentlyValid(c.isCurrentlyValid())
                                .build())))
                .orElse(ResponseEntity.ok(ApiResponse.success("No welcome coupon", null)));
    }

}