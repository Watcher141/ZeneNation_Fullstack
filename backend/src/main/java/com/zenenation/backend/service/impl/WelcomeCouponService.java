package com.zenenation.backend.service.impl;

import com.zenenation.backend.entity.Coupon;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.DiscountType;
import com.zenenation.backend.repository.CouponRepository;
import com.zenenation.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Generates a personal welcome coupon for every new user on registration.
 *
 * Rules:
 * - 5% discount
 * - Max discount cap: ₹200
 * - No minimum order amount
 * - One-time use (usageLimit = 1, perUserLimit = 1)
 * - Assigned to specific user only — no one else can use it
 * - Expires in 30 days
 * - Code format: WELCOME-FIRSTNAME-XXXX (e.g. WELCOME-MONOJIT-X7K2)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WelcomeCouponService {

    private final CouponRepository couponRepository;
    private final EmailService emailService;

    private static final double WELCOME_DISCOUNT_PERCENT = 5.0;
    private static final double MAX_DISCOUNT_AMOUNT      = 200.0;
    private static final int    EXPIRY_DAYS              = 30;

    @Transactional
    public Coupon generateWelcomeCoupon(User user) {
        String code = generateUniqueCode(user.getName());

        Coupon coupon = Coupon.builder()
                .code(code)
                .description("Welcome gift — 5% off your first order!")
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(WELCOME_DISCOUNT_PERCENT))
                .minimumOrderAmount(BigDecimal.ZERO)
                .maximumDiscount(BigDecimal.valueOf(MAX_DISCOUNT_AMOUNT))
                .usageLimit(1)              // Total uses: 1
                .perUserLimit(1)            // Per user: 1
                .usedCount(0)
                .isActive(true)
                .isWelcomeCoupon(true)
                .assignedUser(user)         // Locked to this user only
                .validFrom(LocalDateTime.now())
                .validUntil(LocalDateTime.now().plusDays(EXPIRY_DAYS))
                .build();

        coupon = couponRepository.save(coupon);
        log.info("Welcome coupon generated: code={}, userId={}", code, user.getId());

        // Send welcome email with the coupon code
        emailService.sendWelcomeCouponEmail(
                user.getEmail(),
                user.getName(),
                code,
                EXPIRY_DAYS
        );

        return coupon;
    }

    private String generateUniqueCode(String userName) {
        // Extract first name, uppercase, max 8 chars, letters only
        String firstName = userName.split(" ")[0]
                .toUpperCase()
                .replaceAll("[^A-Z]", "")
                .substring(0, Math.min(8, userName.split(" ")[0].replaceAll("[^A-Za-z]", "").length()));

        // Random 4-char suffix for uniqueness
        String suffix = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 4)
                .toUpperCase();

        String code = "WELCOME-" + firstName + "-" + suffix;

        // Ensure uniqueness — regenerate suffix if collision (extremely rare)
        int attempts = 0;
        while (couponRepository.existsByCode(code) && attempts < 10) {
            suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
            code = "WELCOME-" + firstName + "-" + suffix;
            attempts++;
        }

        return code;
    }
}