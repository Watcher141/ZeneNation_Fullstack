package com.zenenation.backend.enums;

/**
 * Payment method chosen by the customer at checkout.
 *
 * COD           → Cash on Delivery (limited to orders below ₹10,000 — configured in yml)
 * ONLINE        → Any Razorpay supported method:
 *                 UPI, Credit/Debit Card, NetBanking, Wallet, EMI
 *
 * Keeping this as an enum (not a free-text field) ensures:
 * - Admin dashboard can filter orders by payment method
 * - Business logic (e.g. COD limit) can be applied cleanly
 * - No typos or inconsistent values in the database
 */
public enum PaymentMethod {

    /**
     * Cash on Delivery.
     * Not available for orders above the configured COD limit.
     */
    COD,

    /**
     * Online payment via Razorpay.
     * Covers: UPI (GPay, PhonePe, Paytm), Cards, NetBanking,
     *         Wallets, EMI, and International cards.
     */
    ONLINE
}