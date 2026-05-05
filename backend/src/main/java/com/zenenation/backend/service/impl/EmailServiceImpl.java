package com.zenenation.backend.service.impl;

import com.zenenation.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    /** Sender address — set MAIL_FROM env var, falls back to MAIL_USERNAME */
    @Value("${spring.mail.from:${spring.mail.username:noreply@zenenation.com}}")
    private String fromEmail;

    /** Frontend URL for email links — set FRONTEND_URL env var */
    @Value("${app.cors.allowed-origins[2]:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.admin.email:admin@zenenation.com}")
    private String adminEmail;

    // -------------------------------------------------------------------------
    // PASSWORD RESET EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(String.valueOf(fromEmail));
            helper.setTo(String.valueOf(toEmail));
            helper.setSubject("Reset Your Password — Zenenation");
            helper.setText(String.valueOf(buildPasswordResetEmailBody(resetLink)), true);
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // ORDER CONFIRMATION EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendOrderConfirmationEmail(String toEmail, String orderNumber, String totalAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(String.valueOf(fromEmail));
            helper.setTo(String.valueOf(toEmail));
            helper.setSubject("Order Confirmed — " + orderNumber);
            helper.setText(String.valueOf(buildOrderConfirmationEmailBody(orderNumber, totalAmount)), true);
            mailSender.send(message);
            log.info("Order confirmation email sent to: {} for order: {}", toEmail, orderNumber);
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // ABANDONED CART EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendAbandonedCartEmail(String toEmail, String userName, int itemCount, double cartTotal) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(String.valueOf(fromEmail));
            helper.setTo(String.valueOf(toEmail));
            helper.setSubject("You left something behind! 🛒 — Zenenation");
            helper.setText(String.valueOf(buildAbandonedCartBody(userName, itemCount, cartTotal)), true);
            mailSender.send(message);
            log.info("Abandoned cart email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send abandoned cart email to {}: {}", toEmail, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // SUBSCRIPTION WELCOME EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendSubscriptionWelcomeEmail(String toEmail, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(String.valueOf(fromEmail));
            helper.setTo(String.valueOf(toEmail));
            helper.setSubject("Welcome to Zenenation! 🎌");
            helper.setText(String.valueOf(buildSubscriptionWelcomeBody(name)), true);
            mailSender.send(message);
            log.info("Subscription welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send subscription welcome email to {}: {}", toEmail, e.getMessage());
        }
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
        for (String email : emails) {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(String.valueOf(fromEmail));
                helper.setTo(String.valueOf(email));
                helper.setSubject(emoji + " " + title + " — Zenenation");
                helper.setText(String.valueOf(buildAnnouncementBody(title, message, accentColor, emoji)), true);
                mailSender.send(msg);
            } catch (Exception e) {
                log.error("Failed to send announcement email to {}: {}", email, e.getMessage());
            }
        }
        log.info("Announcement email sent to {} subscribers", emails.size());
    }

    // -------------------------------------------------------------------------
    // WELCOME COUPON EMAIL
    // -------------------------------------------------------------------------

    @Override
    @Async
    public void sendWelcomeCouponEmail(String toEmail, String name, String couponCode, int expiryDays) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(String.valueOf(fromEmail));
            helper.setTo(String.valueOf(toEmail));
            helper.setSubject("🎁 Your Welcome Gift — " + couponCode + " | Zenenation");
            helper.setText(String.valueOf(buildWelcomeCouponBody(name, couponCode, expiryDays)), true);
            mailSender.send(message);
            log.info("Welcome coupon email sent to: {} with code: {}", toEmail, couponCode);
        } catch (Exception e) {
            log.error("Failed to send welcome coupon email to {}: {}", toEmail, e.getMessage());
        }
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
                        <p style="font-size:13px;color:#888">⏱ This link expires in <strong>15 minutes</strong>.</p>
                        <p style="font-size:13px;color:#888">If you didn't request this, ignore this email.</p>
                    </div>
                    <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
                </div></body></html>
                """.formatted(resetLink);
    }

    private String buildOrderConfirmationEmailBody(String orderNumber, String totalAmount) {
        return """
                <!DOCTYPE html><html><head><meta charset="UTF-8">
                <style>
                    body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                    .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                    .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                    .body{padding:40px 30px;color:#333}
                    .order-box{background:#f9f9f9;border:1px solid #eee;border-radius:6px;padding:20px;margin:20px 0}
                    .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
                </style></head><body>
                <div class="container">
                    <div class="header"><h1>Order Confirmed! 🎉</h1></div>
                    <div class="body">
                        <p>Thank you for your order!</p>
                        <div class="order-box">
                            <p><strong>Order Number:</strong> %s</p>
                            <p><strong>Total Amount:</strong> ₹%s</p>
                        </div>
                        <p>We'll notify you once your order is shipped.</p>
                    </div>
                    <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
                </div></body></html>
                """.formatted(orderNumber, totalAmount);
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
                <div class="header"><h1>🛒 Your cart misses you!</h1></div>
                <div class="body">
                    <p>Hi %s,</p>
                    <p>You left <strong>%d item(s)</strong> worth <strong>₹%.2f</strong> in your cart!</p>
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
                <div class="header"><h1>Welcome to Zenenation! 🎌</h1></div>
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
        String siteUrl = frontendUrl;
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
            """.formatted(accentColor, accentColor, accentColor, emoji, title, message, siteUrl);
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
                    <p>Here's an exclusive <strong>5%%%% off</strong> coupon for your first order:</p>
                    <div class="coupon-box">
                        <div style="color:#aaa;font-size:13px;letter-spacing:2px;margin-bottom:8px">YOUR COUPON CODE</div>
                        <div class="coupon-code">%s</div>
                        <div style="color:#e94560;font-size:18px;font-weight:bold;margin-top:8px">5%%%% OFF (up to ₹200)</div>
                        <div style="color:#888;font-size:12px;margin-top:8px">⏰ Valid for %d days · One-time use only</div>
                    </div>
                    <a href="%s" class="btn">Shop Now →</a>
                </div>
                <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
            </div></body></html>
            """.formatted(name, couponCode, expiryDays, shopUrl);
    }
}