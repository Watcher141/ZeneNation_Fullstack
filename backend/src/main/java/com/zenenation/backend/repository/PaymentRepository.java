package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Payment;
import com.zenenation.backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by order ID.
     * Used when verifying Razorpay payment after checkout.
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Find payment by Razorpay order ID.
     * Used in Razorpay webhook handler to match incoming
     * webhook events to the correct payment record.
     */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /**
     * Find payment by Razorpay payment ID.
     * Used when processing refunds — Razorpay needs paymentId to refund.
     */
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    /**
     * Check if a payment exists for an order with a given status.
     * Used to prevent duplicate payment attempts on the same order.
     * Example: block re-payment if payment is already PAID.
     */
    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
