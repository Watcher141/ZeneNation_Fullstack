package com.zenenation.backend.enums;

/**
 * Tracks the payment state independently from the order state.
 *
 * Why separate from OrderStatus?
 * Because an order and its payment can be in different states.
 * Example: Order is SHIPPED but payment is still PENDING (COD case).
 *          Order is CANCELLED but payment is REFUND_INITIATED.
 *
 * This separation gives the admin full visibility into both
 * the logistics state AND the financial state of every order.
 */
public enum PaymentStatus {

    /**
     * Payment not yet made.
     * COD: will remain PENDING until delivery.
     * Online: waiting for user to complete payment.
     */
    PENDING,

    /**
     * Payment successfully received and verified.
     * Online: Razorpay signature verified.
     * COD: marked paid by admin on delivery.
     */
    PAID,

    /**
     * Payment attempt was made but failed.
     * (Card declined, UPI timeout, net banking error etc.)
     */
    FAILED,

    /**
     * Refund has been initiated (e.g. after cancellation/return).
     * Money is on its way back but not yet in customer's account.
     */
    REFUND_INITIATED,

    /**
     * Refund successfully credited back to customer.
     */
    REFUNDED
}