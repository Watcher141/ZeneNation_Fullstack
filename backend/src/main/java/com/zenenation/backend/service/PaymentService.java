package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.PaymentVerificationRequest;
import com.zenenation.backend.dto.response.PaymentResponse;

public interface PaymentService {

    /**
     * Verify a Razorpay payment after user completes checkout.
     *
     * Verifies the HMAC-SHA256 signature using:
     *   razorpayOrderId + "|" + razorpayPaymentId
     * signed with our Razorpay key secret.
     *
     * If valid   → marks Payment as PAID, Order as CONFIRMED
     * If invalid → marks Payment as FAILED, Order as PAYMENT_FAILED
     */
    PaymentResponse verifyPayment(PaymentVerificationRequest request);

    /** Get payment details for a specific order (current user only) */
    PaymentResponse getPaymentByOrderId(Long orderId);
}
