package com.zenenation.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a payment operation fails.
 * Maps to HTTP 402 Payment Required.
 *
 * Usage examples:
 *   throw new PaymentException("Payment verification failed — invalid signature");
 *   throw new PaymentException("Failed to create Razorpay order");
 *   throw new PaymentException("Refund initiation failed");
 */
public class PaymentException extends BaseException {

    public PaymentException(String message) {
        super(message, HttpStatus.PAYMENT_REQUIRED);
    }
}
