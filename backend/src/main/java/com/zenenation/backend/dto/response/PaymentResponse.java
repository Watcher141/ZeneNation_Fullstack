package com.zenenation.backend.dto.response;

import com.zenenation.backend.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment details returned inside OrderResponse.
 * razorpaySignature is intentionally excluded — security sensitive.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String failureReason;       // shown if payment failed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
