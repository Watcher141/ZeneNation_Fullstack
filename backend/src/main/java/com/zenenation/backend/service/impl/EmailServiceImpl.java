package com.zenenation.backend.service.impl;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.zenenation.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sends emails via Resend HTTP API (primary).
 * SMTP config is kept in application.yml for future use.
 *
 * Resend works on Render free tier — uses HTTPS (port 443).
 * SMTP was blocked by Render on ports 587 and 465.
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${resend.api-key:re_placeholder}")
    private String resendApiKey;

    @Value("${resend.from:Zenenation <noreply@zenenation.com>}")
    private String fromEmail;

    @Value("${app.cors.allowed-origins[2]:http://localhost:5173}")
    private String frontendUrl;

    private Resend getResend() {
        return new Resend(resendApiKey);
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .build();
            getResend().emails().send(params);
            log.info("Email sent via Resend to: {}", to);
        } catch (ResendException e) {
            log.error("Failed to send email via Resend to {}: {}", to, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // PASSWORD RESET EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        sendEmail(toEmail, "Reset Your Password — Zenenation",
                buildPasswordResetEmailBody(resetLink));
    }

    // -------------------------------------------------------------------------
    // ORDER CONFIRMATION EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendOrderConfirmationEmail(String toEmail, String orderNumber, String totalAmount) {
        sendEmail(toEmail, "Order Confirmed ✅ — " + orderNumber,
                buildOrderConfirmationEmailBody(orderNumber, totalAmount));
    }

    // -------------------------------------------------------------------------
    // NEW ORDER — ADMIN NOTIFICATION
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendNewOrderAdminEmail(String adminEmail, String orderNumber,
                                       String customerName, String totalAmount,
                                       String paymentMethod) {
        sendEmail(adminEmail, "🛒 New Order Received — " + orderNumber,
                buildNewOrderAdminEmailBody(orderNumber, customerName, totalAmount, paymentMethod));
    }

    // -------------------------------------------------------------------------
    // ORDER CANCELLATION — ADMIN NOTIFICATION
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendOrderCancellationAdminEmail(String adminEmail, String orderNumber,
                                                String customerName, String totalAmount) {
        sendEmail(adminEmail, "❌ Order Cancelled — " + orderNumber,
                buildOrderCancellationAdminEmailBody(orderNumber, customerName, totalAmount));
    }

    // -------------------------------------------------------------------------
    // ABANDONED CART EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendAbandonedCartEmail(String toEmail, String userName, int itemCount, double cartTotal) {
        sendEmail(toEmail, "You left something behind! — Zenenation",
                buildAbandonedCartBody(userName, itemCount, cartTotal));
    }

    // -------------------------------------------------------------------------
    // SUBSCRIPTION WELCOME EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendSubscriptionWelcomeEmail(String toEmail, String name) {
        sendEmail(toEmail, "Welcome to Zenenation!",
                buildSubscriptionWelcomeBody(name));
    }

    // -------------------------------------------------------------------------
    // ANNOUNCEMENT BLAST
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendAnnouncementEmail(List<String> emails, String title, String message, String type) {
        String accentColor = switch (type) {
            case "DEAL"    -> "#f5a623";
            case "WARNING" -> "#ff9800";
            case "SUCCESS" -> "#4caf50";
            default        -> "#e94560";
        };
        String emoji = switch (type) {
            case "DEAL"    -> "🎉";
            case "WARNING" -> "⚠️";
            case "SUCCESS" -> "✅";
            default        -> "📢";
        };
        String html = buildAnnouncementBody(title, message, accentColor, emoji);
        String subject = emoji + " " + title + " — Zenenation";
        for (String email : emails) {
            sendEmail(email, subject, html);
        }
        log.info("Announcement email sent to {} subscribers", emails.size());
    }

    // -------------------------------------------------------------------------
    // WELCOME COUPON EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendWelcomeCouponEmail(String toEmail, String name, String couponCode, int expiryDays) {
        sendEmail(toEmail, "Your Welcome Gift — " + couponCode + " | Zenenation",
                buildWelcomeCouponBody(name, couponCode, expiryDays));
    }

    // =========================================================================
    // EMAIL TEMPLATES
    // =========================================================================

    private String buildPasswordResetEmailBody(String resetLink) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .body p{line-height:1.6;margin:0 0 16px}
                .btn{display:inline-block;background:#e94560;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>Zenenation</h1></div>
                <div class="body">
                    <p>Hi there,</p>
                    <p>We received a request to reset your password. Click the button below:</p>
                    <a href="%s" class="btn">Reset My Password</a>
                    <p style="font-size:13px;color:#888">This link expires in <strong>15 minutes</strong>.</p>
                    <p style="font-size:13px;color:#888">If you didn't request this, ignore this email.</p>
                </div>
                <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
            </div></body></html>
            """.formatted(resetLink);
    }

    private String buildOrderConfirmationEmailBody(String orderNumber, String totalAmount) {
        String ordersUrl = frontendUrl + "/orders";
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .order-box{background:#f9f9f9;border:1px solid #eee;border-left:4px solid #e94560;border-radius:6px;padding:20px;margin:20px 0}
                .order-box p{margin:6px 0;font-size:15px}
                .btn{display:inline-block;background:#e94560;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>🎉 Order Confirmed!</h1><p style="margin:4px 0;opacity:.8">Zenenation</p></div>
                <div class="body">
                    <p>Thank you for your order! We've received it and it's being processed.</p>
                    <div class="order-box">
                        <p><strong>Order Number:</strong> %s</p>
                        <p><strong>Total Amount:</strong> ₹%s</p>
                    </div>
                    <p>We'll send you another email as soon as your order is shipped. You can track your order anytime:</p>
                    <a href="%s" class="btn">View My Orders</a>
                    <p style="font-size:13px;color:#888">If you have any questions, reply to this email or contact our support team.</p>
                </div>
                <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
            </div></body></html>
            """.formatted(orderNumber, totalAmount, ordersUrl);
    }

    private String buildNewOrderAdminEmailBody(String orderNumber, String customerName,
                                               String totalAmount, String paymentMethod) {
        String adminOrdersUrl = frontendUrl + "/admin/orders";
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#0f3460;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .order-box{background:#f0f4ff;border:1px solid #c5d5f5;border-left:4px solid #0f3460;border-radius:6px;padding:20px;margin:20px 0}
                .order-box p{margin:8px 0;font-size:15px}
                .badge{display:inline-block;padding:4px 12px;border-radius:20px;font-size:12px;font-weight:bold;margin-top:4px}
                .badge-cod{background:#fff3cd;color:#856404}
                .badge-online{background:#d1fae5;color:#065f46}
                .btn{display:inline-block;background:#0f3460;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>🛒 New Order Received!</h1><p style="margin:4px 0;opacity:.8">Zenenation Admin</p></div>
                <div class="body">
                    <p>A new order has been placed on your store.</p>
                    <div class="order-box">
                        <p><strong>Order Number:</strong> %s</p>
                        <p><strong>Customer:</strong> %s</p>
                        <p><strong>Total Amount:</strong> ₹%s</p>
                        <p><strong>Payment:</strong> <span class="badge %s">%s</span></p>
                    </div>
                    <a href="%s" class="btn">View Orders Dashboard</a>
                </div>
                <div class="footer">&copy; 2025 Zenenation Admin Panel. All rights reserved.</div>
            </div></body></html>
            """.formatted(
                orderNumber, customerName, totalAmount,
                "COD".equals(paymentMethod) ? "badge-cod" : "badge-online",
                "COD".equals(paymentMethod) ? "Cash on Delivery" : "Online Payment",
                adminOrdersUrl
        );
    }

    private String buildAbandonedCartBody(String userName, int itemCount, double cartTotal) {
        String cartUrl = frontendUrl + "/cart";
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .btn{display:inline-block;background:#e94560;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>Your cart misses you!</h1></div>
                <div class="body">
                    <p>Hi %s,</p>
                    <p>You left <strong>%d item(s)</strong> worth <strong>Rs.%.2f</strong> in your cart!</p>
                    <a href="%s" class="btn">Complete My Order</a>
                </div>
                <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
            </div></body></html>
            """.formatted(userName, itemCount, cartTotal, cartUrl);
    }

    private String buildSubscriptionWelcomeBody(String name) {
        String shopUrl = frontendUrl + "/products";
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .feature{display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid #eee}
                .btn{display:inline-block;background:#e94560;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>Welcome to Zenenation!</h1></div>
                <div class="body">
                    <p>Hi %s,</p>
                    <p>You're now subscribed! Here's what you'll receive:</p>
                    <div class="feature"><span>🎉</span><span>Exclusive deals and discounts</span></div>
                    <div class="feature"><span>📦</span><span>New product announcements</span></div>
                    <div class="feature"><span>⚡</span><span>Flash sale alerts</span></div>
                    <div class="feature"><span>🏷️</span><span>Special member-only offers</span></div>
                    <a href="%s" class="btn">Shop Now</a>
                </div>
                <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
            </div></body></html>
            """.formatted(name != null ? name : "Anime Fan", shopUrl);
    }

    private String buildAnnouncementBody(String title, String message, String accentColor, String emoji) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .accent-bar{background:%s;height:4px}
                .body{padding:40px 30px;color:#333}
                .message-box{background:#f9f9f9;border-left:4px solid %s;border-radius:4px;padding:20px;margin:20px 0;font-size:16px;line-height:1.6}
                .btn{display:inline-block;background:%s;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>%s %s</h1></div>
                <div class="accent-bar"></div>
                <div class="body">
                    <div class="message-box">%s</div>
                    <a href="%s" class="btn">Visit Zenenation</a>
                </div>
                <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
            </div></body></html>
            """.formatted(accentColor, accentColor, accentColor, emoji, title, message, frontendUrl);
    }

    private String buildWelcomeCouponBody(String name, String couponCode, int expiryDays) {
        String shopUrl = frontendUrl + "/products";
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .body p{line-height:1.6;margin:0 0 16px}
                .coupon-box{background:#1a1a2e;border:2px dashed #e94560;border-radius:12px;padding:30px;text-align:center;margin:24px 0}
                .coupon-code{font-size:32px;font-weight:bold;color:#f5a623;letter-spacing:4px;font-family:monospace}
                .btn{display:inline-block;background:#e94560;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header">
                    <p style="font-size:3rem;margin:0">🎌</p>
                    <h1>Welcome to Zenenation!</h1>
                </div>
                <div class="body">
                    <p>Hi <strong>%s</strong>,</p>
                    <p>Here's an exclusive <strong>5%% off</strong> coupon for your first order:</p>
                    <div class="coupon-box">
                        <div style="color:#aaa;font-size:13px;letter-spacing:2px;margin-bottom:8px">YOUR COUPON CODE</div>
                        <div class="coupon-code">%s</div>
                        <div style="color:#e94560;font-size:18px;font-weight:bold;margin-top:8px">5%% OFF (up to Rs.200)</div>
                        <div style="color:#888;font-size:12px;margin-top:8px">Valid for %d days · One-time use only</div>
                    </div>
                    <a href="%s" class="btn">Shop Now</a>
                </div>
                <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
            </div></body></html>
            """.formatted(name, couponCode, expiryDays, shopUrl);
    }

    private String buildOrderCancellationAdminEmailBody(String orderNumber,
                                                         String customerName,
                                                         String totalAmount) {
        String adminOrdersUrl = frontendUrl + "/admin/orders";
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#7f1d1d;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .order-box{background:#fff5f5;border:1px solid #fecaca;border-left:4px solid #dc2626;border-radius:6px;padding:20px;margin:20px 0}
                .order-box p{margin:8px 0;font-size:15px}
                .btn{display:inline-block;background:#7f1d1d;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>❌ Order Cancelled</h1><p style="margin:4px 0;opacity:.8">Zenenation Admin</p></div>
                <div class="body">
                    <p>A customer has cancelled their order. Please update your inventory or records if needed.</p>
                    <div class="order-box">
                        <p><strong>Order Number:</strong> %s</p>
                        <p><strong>Customer:</strong> %s</p>
                        <p><strong>Order Value:</strong> ₹%s</p>
                    </div>
                    <a href="%s" class="btn">View Orders Dashboard</a>
                </div>
                <div class="footer">&copy; 2025 Zenenation Admin Panel. All rights reserved.</div>
            </div></body></html>
            """.formatted(orderNumber, customerName, totalAmount, adminOrdersUrl);
    }
}