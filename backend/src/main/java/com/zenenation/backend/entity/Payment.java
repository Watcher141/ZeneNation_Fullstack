package com.zenenation.backend.entity;

import com.zenenation.backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment record for an Order.
 *
 * For ONLINE payments (Razorpay):
 *  1. We call Razorpay API → get razorpayOrderId → store here
 *  2. User completes payment on frontend
 *  3. Frontend sends razorpayPaymentId + razorpaySignature back to us
 *  4. We verify the signature using HMAC-SHA256
 *  5. If valid → mark PaymentStatus.PAID, update Order status to CONFIRMED
 *  6. If invalid → mark PaymentStatus.FAILED
 *
 * For COD:
 *  No Razorpay involvement. Payment record is created with PENDING status.
 *  Admin marks it PAID when delivery is confirmed.
 *
 * All Razorpay IDs are stored for:
 *  - Customer support (trace any payment issue)
 *  - Refund processing (need razorpayPaymentId to initiate refund)
 *  - Reconciliation with Razorpay dashboard
 */
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id"),
        // Fast lookup by Razorpay IDs (used in webhook handling)
        @Index(name = "idx_payments_razorpay_order_id", columnList = "razorpay_order_id"),
        @Index(name = "idx_payments_razorpay_payment_id", columnList = "razorpay_payment_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // ORDER REFERENCE
    // ------------------------------------------------------------------

    /**
     * The order this payment belongs to.
     * OneToOne — one payment record per order.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // ------------------------------------------------------------------
    // RAZORPAY IDs (null for COD)
    // ------------------------------------------------------------------

    /**
     * Razorpay's order ID — created when we initiate payment.
     * Format: order_XXXXXXXXXXXXXXXXXX
     * Used by frontend to open Razorpay checkout modal.
     */
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    /**
     * Razorpay's payment ID — received after user completes payment.
     * Format: pay_XXXXXXXXXXXXXXXXXX
     * Required to initiate refunds.
     */
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    /**
     * Razorpay's signature — used to verify payment authenticity.
     * We verify: HMAC-SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
     * Stored for audit trail — proves we verified the payment.
     */
    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    // ------------------------------------------------------------------
    // PAYMENT DETAILS
    // ------------------------------------------------------------------

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Currency code. INR for India, supports USD etc. for international.
     */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * Razorpay refund ID — populated when refund is initiated.
     * Format: rfnd_XXXXXXXXXXXXXXXXXX
     */
    @Column(name = "refund_id")
    private String refundId;

    /**
     * Failure reason from Razorpay if payment failed.
     * Example: "Payment failed due to insufficient funds"
     * Useful for customer support and debugging.
     */
    @Column(name = "failure_reason")
    private String failureReason;

    // ------------------------------------------------------------------
    // AUDIT
    // ------------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
