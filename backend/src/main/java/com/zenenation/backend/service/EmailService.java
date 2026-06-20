package com.zenenation.backend.service;

import java.util.List;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String resetLink);

    /** Order confirmation to the customer */
    void sendOrderConfirmationEmail(String toEmail, String orderNumber, String totalAmount);

    /** New order notification to the admin */
    void sendNewOrderAdminEmail(String adminEmail, String orderNumber,
                                String customerName, String totalAmount,
                                String paymentMethod);

    /** Order cancellation notification to the admin */
    void sendOrderCancellationAdminEmail(String adminEmail, String orderNumber,
                                         String customerName, String totalAmount);

    /** Abandoned cart reminder — sent 30 min after cart was last updated */
    void sendAbandonedCartEmail(String toEmail, String userName, int itemCount, double cartTotal);

    /** Welcome email after newsletter subscription */
    void sendSubscriptionWelcomeEmail(String toEmail, String name);

    /** Announcement blast to all subscribers */
    void sendAnnouncementEmail(List<String> emails, String title, String message, String type);

    /** Welcome coupon email sent on registration */
    void sendWelcomeCouponEmail(String toEmail, String name, String couponCode, int expiryDays);
}