package com.zenenation.backend.dto.request;

import com.zenenation.backend.enums.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sent by user when placing an order.
 *
 * addressId       — the saved address to deliver to.
 * paymentMethod   — COD or ONLINE.
 * couponCode      — optional coupon to apply discount (nullable).
 * redeemPoints    — optional reward points to redeem at checkout (0 or null = skip).
 *                   Discount = redeemPoints / 2 rupees (2 pts = ₹1).
 *                   Requires minimum order of ₹399 and max 60% of balance.
 */
@Data
public class PlaceOrderRequest {

    @NotNull(message = "Delivery address is required")
    private Long addressId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Optional — coupon code to apply discount at checkout
    private String couponCode;

    /**
     * For preorder items:
     * HALF → pay 50% now, 50% on shipping
     * FULL → pay 100% now
     * null → regular order
     */
    private String preorderPaymentType;

    // Optional delivery note from user
    private String userNote;

    // Optional — reward points to redeem (0 or null means don't use rewards)
    @Min(value = 0, message = "Redeem points cannot be negative")
    private Integer redeemPoints;
}