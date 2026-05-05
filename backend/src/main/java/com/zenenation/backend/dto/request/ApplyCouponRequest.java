package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent by user when applying a coupon at checkout.
 * Also used to validate a coupon before placing the order
 * so frontend can show the discount preview.
 */
@Data
public class ApplyCouponRequest {

    @NotBlank(message = "Coupon code is required")
    private String code;
}
