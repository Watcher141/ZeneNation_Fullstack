package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.PaymentVerificationRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.PaymentResponse;
import com.zenenation.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Razorpay payment verification")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments/verify
     * Called by frontend after Razorpay checkout completes.
     * Verifies HMAC signature — marks order as CONFIRMED if valid.
     *
     * Frontend flow:
     * 1. User completes payment on Razorpay modal
     * 2. Razorpay returns razorpayOrderId, razorpayPaymentId, razorpaySignature
     * 3. Frontend calls this endpoint with those 3 values
     * 4. We verify and return updated payment status
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment after checkout")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {

        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", response));
    }

    /**
     * GET /api/v1/payments/order/{orderId}
     * Get payment details for a specific order.
     * Only returns payment if the order belongs to the current user.
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment details for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrder(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(ApiResponse.success("Payment fetched",
                paymentService.getPaymentByOrderId(orderId)));
    }
}
