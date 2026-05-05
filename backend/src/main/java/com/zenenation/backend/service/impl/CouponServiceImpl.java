package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.request.ApplyCouponRequest;
import com.zenenation.backend.dto.request.CouponRequest;
import com.zenenation.backend.dto.response.CouponResponse;
import com.zenenation.backend.dto.response.CouponValidationResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.entity.Coupon;
import com.zenenation.backend.entity.CouponUsage;
import com.zenenation.backend.entity.Order;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.DiscountType;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.DuplicateResourceException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.CouponRepository;
import com.zenenation.backend.repository.CouponUsageRepository;
import com.zenenation.backend.repository.OrderRepository;
import com.zenenation.backend.repository.UserRepository;
import com.zenenation.backend.service.CouponService;
import com.zenenation.backend.util.PriceUtil;
import com.zenenation.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final SecurityUtil securityUtil;

    // -------------------------------------------------------------------------
    // ADMIN — CRUD
    // -------------------------------------------------------------------------

    @Override
    public PagedResponse<CouponResponse> getAllCoupons(int page, int size) {
        Page<Coupon> coupons = couponRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return PagedResponse.of(coupons.map(this::toResponse));
    }

    @Override
    public CouponResponse getCouponById(Long id) {
        return toResponse(getCouponEntityById(id));
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        // Normalize code to uppercase
        String code = request.getCode().trim().toUpperCase();

        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Coupon already exists with code: " + code);
        }

        // Validate PERCENTAGE value doesn't exceed 100
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Percentage discount cannot exceed 100%");
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minimumOrderAmount(request.getMinimumOrderAmount() != null
                        ? request.getMinimumOrderAmount() : BigDecimal.ZERO)
                .maximumDiscount(request.getMaximumDiscount())
                .usageLimit(request.getUsageLimit())
                .perUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .validFrom(request.getValidFrom() != null
                        ? request.getValidFrom() : LocalDateTime.now())
                .validUntil(request.getValidUntil())
                .build();

        coupon = couponRepository.save(coupon);
        log.info("Coupon created: code={}, type={}, value={}",
                coupon.getCode(), coupon.getDiscountType(), coupon.getDiscountValue());

        return toResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = getCouponEntityById(id);
        String newCode = request.getCode().trim().toUpperCase();

        // Check code uniqueness — exclude current coupon from check
        couponRepository.findByCodeIgnoreCase(newCode).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException(
                        "Another coupon already exists with code: " + newCode
                );
            }
        });

        coupon.setCode(newCode);
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount() != null
                ? request.getMinimumOrderAmount() : BigDecimal.ZERO);
        coupon.setMaximumDiscount(request.getMaximumDiscount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1);
        if (request.getIsActive() != null) coupon.setIsActive(request.getIsActive());
        if (request.getValidFrom() != null) coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());

        coupon = couponRepository.save(coupon);
        log.info("Coupon updated: id={}, code={}", id, coupon.getCode());
        return toResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse toggleCoupon(Long id) {
        Coupon coupon = getCouponEntityById(id);
        coupon.setIsActive(!coupon.getIsActive());
        coupon = couponRepository.save(coupon);
        log.info("Coupon toggled: id={}, isActive={}", id, coupon.getIsActive());
        return toResponse(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = getCouponEntityById(id);
        couponRepository.delete(coupon);
        log.info("Coupon deleted: id={}, code={}", id, coupon.getCode());
    }

    // -------------------------------------------------------------------------
    // USER — VALIDATE COUPON (preview before order)
    // -------------------------------------------------------------------------

    @Override
    public CouponValidationResponse validateCoupon(
            ApplyCouponRequest request, BigDecimal cartTotal) {

        Long userId = securityUtil.getCurrentUserId();
        Coupon coupon = validateAndGetCoupon(request.getCode(), cartTotal, userId);

        BigDecimal discountAmount = calculateDiscount(coupon, cartTotal);
        BigDecimal finalAmount = PriceUtil.calculateOrderTotal(
                cartTotal, BigDecimal.ZERO, discountAmount
        );

        String message = coupon.getDiscountType() == DiscountType.PERCENTAGE
                ? String.format("Coupon applied! %.0f%% off — You save ₹%.2f",
                        coupon.getDiscountValue(), discountAmount)
                : String.format("Coupon applied! You save ₹%.2f", discountAmount);

        return CouponValidationResponse.builder()
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discountAmount)
                .originalAmount(cartTotal)
                .finalAmount(finalAmount)
                .message(message)
                .build();
    }

    // -------------------------------------------------------------------------
    // INTERNAL — called from OrderService
    // -------------------------------------------------------------------------

    @Override
    public Coupon validateAndGetCoupon(String code, BigDecimal cartTotal, Long userId) {
        // Find coupon
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException(
                        "Coupon code '" + code + "' is invalid or does not exist"
                ));

        // Check if coupon is currently valid (active, within date range, has uses left)
        if (!coupon.isCurrentlyValid()) {
            throw new BadRequestException(
                    "Coupon '" + code + "' is expired or no longer available"
            );
        }

        // Check minimum order amount
        if (cartTotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new BadRequestException(
                    String.format("Minimum order amount of ₹%.2f required to use this coupon",
                            coupon.getMinimumOrderAmount())
            );
        }

        // Check per-user usage limit
        int userUsageCount = couponUsageRepository.countByCouponIdAndUserId(
                coupon.getId(), userId
        );
        if (userUsageCount >= coupon.getPerUserLimit()) {
            throw new BadRequestException(
                    "You have already used this coupon the maximum number of times"
            );
        }

        return coupon;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discountAmount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            // discountAmount = subtotal × (discountValue / 100)
            discountAmount = subtotal
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            // Apply maximum discount cap if set
            if (coupon.getMaximumDiscount() != null
                    && discountAmount.compareTo(coupon.getMaximumDiscount()) > 0) {
                discountAmount = coupon.getMaximumDiscount();
            }
        } else {
            // FLAT discount — just the fixed amount, but never more than subtotal
            discountAmount = coupon.getDiscountValue().min(subtotal);
        }

        return PriceUtil.round(discountAmount);
    }

    @Override
    @Transactional
    public void recordCouponUsage(Coupon coupon, Long userId, Long orderId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Save usage record
        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .order(order)
                .build();
        couponUsageRepository.save(usage);

        // Increment global usage count
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        log.info("Coupon usage recorded: code={}, userId={}, orderId={}",
                coupon.getCode(), userId, orderId);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Coupon getCouponEntityById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    }

    private CouponResponse toResponse(Coupon coupon) {
        Integer remainingUses = coupon.getUsageLimit() != null
                ? Math.max(0, coupon.getUsageLimit() - coupon.getUsedCount())
                : null;

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .perUserLimit(coupon.getPerUserLimit())
                .isActive(coupon.getIsActive())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .isCurrentlyValid(coupon.isCurrentlyValid())
                .remainingUses(remainingUses)
                .build();
    }
}
