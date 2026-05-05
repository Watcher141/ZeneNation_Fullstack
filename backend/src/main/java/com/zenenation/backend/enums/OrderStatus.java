package com.zenenation.backend.enums;

/**
 * Tracks the lifecycle of an order from placement to delivery.
 *
 * Flow for COD:
 *   PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 *   At any point before SHIPPED → CANCELLED
 *
 * Flow for Online Payment:
 *   PENDING → PAYMENT_FAILED (if payment fails, order dies here)
 *   PENDING → CONFIRMED (once payment verified) → PROCESSING → SHIPPED → DELIVERED
 *
 * RETURNED is for post-delivery return requests.
 */
public enum OrderStatus {

    /**
     * Order placed but not yet confirmed.
     * COD: waiting for admin confirmation.
     * Online: waiting for payment verification.
     */
    PENDING,

    /**
     * Order confirmed.
     * COD: admin accepted the order.
     * Online: payment verified successfully.
     */
    CONFIRMED,

    /**
     * Order is being packed / prepared in warehouse.
     */
    PROCESSING,

    /**
     * Order handed over to courier. Tracking available.
     */
    SHIPPED,

    /**
     * Order delivered to customer.
     */
    DELIVERED,

    /**
     * Order cancelled (by user or admin).
     * Only allowed before SHIPPED status.
     */
    CANCELLED,

    /**
     * Online payment attempt failed.
     * Order stays here until user retries or it expires.
     */
    PAYMENT_FAILED,

    /**
     * Customer requested a return after delivery.
     */
    RETURNED
}