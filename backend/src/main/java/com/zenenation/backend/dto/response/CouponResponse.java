package com.zenenation.backend.dto.response;

import com.zenenation.backend.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Returned for admin coupon management views.
 * Contains full coupon details including usage stats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {

    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscount;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer perUserLimit;
    private Boolean isActive;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed fields
    private Boolean isCurrentlyValid;       // Is it usable right now?
    private Integer remainingUses;          // NULL if unlimited
}
