package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Sent by frontend after user completes Razorpay payment.
 * We use these 3 values to verify the payment signature.
 *
 * Flow:
 * 1. User clicks "Pay Now" → frontend gets razorpayOrderId from our API
 * 2. Razorpay modal opens, user pays
 * 3. Razorpay returns these 3 values to frontend
 * 4. Frontend sends them to our /api/v1/payments/verify endpoint
 * 5. We verify HMAC signature → mark order as CONFIRMED
 */
@Data
public class PaymentVerificationRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}
