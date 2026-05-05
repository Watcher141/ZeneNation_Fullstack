package com.zenenation.backend.dto.response;

import com.zenenation.backend.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Returned when user applies/validates a coupon at checkout.
 * Frontend uses this to show the discount preview before order placement.
 *
 * Example UI:
 * "Coupon SAVE20 applied! You save ₹200.00"
 * Original: ₹1000  →  After discount: ₹800
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponValidationResponse {

    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;       // The percentage or flat amount
    private BigDecimal discountAmount;      // Actual rupee savings on this cart
    private BigDecimal originalAmount;      // Cart total before discount
    private BigDecimal finalAmount;         // Cart total after discount
    private String message;                 // e.g. "Coupon applied! You save ₹200"
}
