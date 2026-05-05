package com.zenenation.backend.dto.request;

import com.zenenation.backend.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Used by admin to CREATE or UPDATE a coupon.
 */
@Data
public class CouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
    @Pattern(
        regexp = "^[A-Z0-9_-]+$",
        message = "Coupon code must be uppercase letters, numbers, hyphens or underscores only"
    )
    private String code;                    // e.g. SAVE20, FLAT100, LAUNCH_50

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;      // PERCENTAGE or FLAT

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;       // 20.00 = 20% OR ₹20

    @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    // Only relevant for PERCENTAGE type — caps the max rupee discount
    @DecimalMin(value = "0.01", message = "Maximum discount must be greater than 0")
    private BigDecimal maximumDiscount;     // e.g. max ₹500 even if 20% = ₹700

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;             // NULL = unlimited

    @Min(value = 1, message = "Per-user limit must be at least 1")
    private Integer perUserLimit = 1;

    private Boolean isActive = true;

    private LocalDateTime validFrom;        // NULL = immediately active

    private LocalDateTime validUntil;       // NULL = no expiry
}
