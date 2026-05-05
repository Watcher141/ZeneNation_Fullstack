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

/**
 * Sends emails via Gmail SMTP (configured in application.yml).
 *
 * @Async — emails are sent in a background thread.
 * The API response returns immediately without waiting for email delivery.
 * This prevents slow email servers from delaying API responses.
 *
 * GMAIL SETUP:
 * 1. Enable 2-Factor Authentication on your Gmail account
 * 2. Go to Google Account → Security → App Passwords
 * 3. Generate an App Password for "Mail"
 * 4. Use that App Password in MAIL_PASSWORD env var (not your Gmail password)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin.email:admin@ecommerce.com}")
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
            helper.setText(String.valueOf(buildPasswordResetEmailBody(resetLink)), true); // true = HTML

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (Exception e) {
            // Log but don't throw — email failure should not crash the API
            // User can request another reset link
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
    // EMAIL TEMPLATES
    // -------------------------------------------------------------------------

    private String buildPasswordResetEmailBody(String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: white;
                                     border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background: #1a1a2e; color: white; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; }
                        .body { padding: 40px 30px; color: #333; }
                        .body p { line-height: 1.6; margin: 0 0 16px; }
                        .btn { display: inline-block; background: #e94560; color: white;
                               padding: 14px 32px; border-radius: 6px; text-decoration: none;
                               font-weight: bold; font-size: 16px; margin: 20px 0; }
                        .note { font-size: 13px; color: #888; margin-top: 24px; }
                        .footer { background: #f4f4f4; padding: 20px; text-align: center;
                                  font-size: 12px; color: #aaa; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Zenenation</h1>
                        </div>
                        <div class="body">
                            <p>Hi there,</p>
                            <p>We received a request to reset your password. Click the button below to create a new password:</p>
                            <a href="%s" class="btn">Reset My Password</a>
                            <p class="note">⏱ This link expires in <strong>15 minutes</strong>.</p>
                            <p class="note">If you didn't request a password reset, you can safely ignore this email. Your password will not be changed.</p>
                        </div>
                        <div class="footer">
                            &copy; 2024 Zenenation. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resetLink);
    }

    private String buildOrderConfirmationEmailBody(String orderNumber, String totalAmount) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: white;
                                     border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        .header { background: #1a1a2e; color: white; padding: 30px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; }
                        .body { padding: 40px 30px; color: #333; }
                        .body p { line-height: 1.6; margin: 0 0 16px; }
                        .order-box { background: #f9f9f9; border: 1px solid #eee;
                                     border-radius: 6px; padding: 20px; margin: 20px 0; }
                        .footer { background: #f4f4f4; padding: 20px; text-align: center;
                                  font-size: 12px; color: #aaa; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Order Confirmed! 🎉</h1>
                        </div>
                        <div class="body">
                            <p>Thank you for your order!</p>
                            <div class="order-box">
                                <p><strong>Order Number:</strong> %s</p>
                                <p><strong>Total Amount:</strong> ₹%s</p>
                            </div>
                            <p>We'll notify you once your order is shipped.</p>
                        </div>
                        <div class="footer">
                            &copy; 2024 Zenenation. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(orderNumber, totalAmount);
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
    // EMAIL TEMPLATES
    // -------------------------------------------------------------------------

    private String buildAbandonedCartBody(String userName, int itemCount, double cartTotal) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .body p{line-height:1.6;margin:0 0 16px}
                .btn{display:inline-block;background:#e94560;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .cart-box{background:#f9f9f9;border:1px solid #eee;border-radius:6px;padding:20px;margin:20px 0;text-align:center}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>🛒 Your cart misses you!</h1></div>
                <div class="body">
                    <p>Hi %s,</p>
                    <p>You left <strong>%d item(s)</strong> worth <strong>₹%.2f</strong> in your cart. They're still waiting for you!</p>
                    <div class="cart-box">
                        <p style="font-size:2rem;margin:0">🛒</p>
                        <p><strong>%d item(s) in your cart</strong></p>
                        <p style="color:#e94560;font-size:1.2rem;font-weight:bold">₹%.2f</p>
                    </div>
                    <p>Don't wait too long — popular items sell out fast!</p>
                    <a href="http://localhost:5173/cart" class="btn">Complete My Order</a>
                </div>
                <div class="footer">&copy; 2024 Zenenation. <a href="#">Unsubscribe</a></div>
            </div>
            </body></html>
            """.formatted(userName, itemCount, cartTotal, itemCount, cartTotal);
    }

    private String buildSubscriptionWelcomeBody(String name) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .body{padding:40px 30px;color:#333}
                .body p{line-height:1.6;margin:0 0 16px}
                .feature{display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid #eee}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>Welcome to Zenenation! 🎌</h1></div>
                <div class="body">
                    <p>Hi %s,</p>
                    <p>You're now subscribed to Zenenation updates! Here's what you'll receive:</p>
                    <div class="feature"><span>🎉</span><span>Exclusive deals and discounts</span></div>
                    <div class="feature"><span>📦</span><span>New product announcements</span></div>
                    <div class="feature"><span>⚡</span><span>Flash sale alerts</span></div>
                    <div class="feature"><span>🏷️</span><span>Special member-only offers</span></div>
                    <p style="margin-top:24px">Stay tuned for amazing anime merchandise deals!</p>
                </div>
                <div class="footer">&copy; 2024 Zenenation. <a href="#">Unsubscribe</a></div>
            </div>
            </body></html>
            """.formatted(name != null ? name : "Anime Fan");
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
                .body p{line-height:1.6;margin:0 0 16px}
                .message-box{background:#f9f9f9;border-left:4px solid %s;border-radius:4px;padding:20px;margin:20px 0;font-size:16px;line-height:1.6}
                .btn{display:inline-block;background:%s;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header"><h1>%s %s</h1></div>
                <div class="accent-bar"></div>
                <div class="body">
                    <div class="message-box">%s</div>
                    <a href="http://localhost:5173" class="btn">Visit Zenenation</a>
                </div>
                <div class="footer">&copy; 2024 Zenenation. <a href="#">Unsubscribe</a></div>
            </div>
            </body></html>
            """.formatted(accentColor, accentColor, accentColor, emoji, title, message);
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

    private String buildWelcomeCouponBody(String name, String couponCode, int expiryDays) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
                body{font-family:Arial,sans-serif;background:#f4f4f4;margin:0;padding:0}
                .container{max-width:600px;margin:40px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.1)}
                .header{background:#1a1a2e;color:white;padding:30px;text-align:center}
                .header h1{margin:0;font-size:24px}
                .body{padding:40px 30px;color:#333}
                .body p{line-height:1.6;margin:0 0 16px}
                .coupon-box{background:#1a1a2e;border:2px dashed #e94560;border-radius:12px;padding:30px;text-align:center;margin:24px 0}
                .coupon-label{color:#aaa;font-size:13px;text-transform:uppercase;letter-spacing:2px;margin-bottom:8px}
                .coupon-code{font-size:32px;font-weight:bold;color:#f5a623;letter-spacing:4px;font-family:monospace}
                .coupon-discount{color:#e94560;font-size:18px;font-weight:bold;margin-top:8px}
                .coupon-expiry{color:#888;font-size:12px;margin-top:8px}
                .btn{display:inline-block;background:#e94560;color:white;padding:14px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:16px;margin:20px 0}
                .perks{background:#f9f9f9;border-radius:8px;padding:20px;margin:20px 0}
                .perk{display:flex;align-items:center;gap:12px;padding:8px 0;font-size:14px;color:#555}
                .footer{background:#f4f4f4;padding:20px;text-align:center;font-size:12px;color:#aaa}
            </style></head><body>
            <div class="container">
                <div class="header">
                    <p style="font-size:3rem;margin:0">🎌</p>
                    <h1>Welcome to Zenenation!</h1>
                    <p style="margin:8px 0 0;color:#aaa;font-size:14px">Here's a little gift from us</p>
                </div>
                <div class="body">
                    <p>Hi <strong>%s</strong>,</p>
                    <p>Thank you for joining the Zenenation family! We're so excited to have you here.</p>
                    <p>As a welcome gift, here's an exclusive <strong>5%% off</strong> coupon for your first order — just for you:</p>

                    <div class="coupon-box">
                        <div class="coupon-label">Your Personal Coupon Code</div>
                        <div class="coupon-code">%s</div>
                        <div class="coupon-discount">5%% OFF (up to ₹200)</div>
                        <div class="coupon-expiry">⏰ Valid for %d days · One-time use only</div>
                    </div>

                    <div class="perks">
                        <div class="perk"><span>✅</span><span>Apply at checkout — no minimum order required</span></div>
                        <div class="perk"><span>🔒</span><span>This coupon is exclusively yours — it won't work for anyone else</span></div>
                        <div class="perk"><span>⚡</span><span>One-time use — make it count!</span></div>
                    </div>

                    <p>Ready to start shopping?</p>
                    <a href="http://localhost:5173/products" class="btn">Shop Now →</a>
                </div>
                <div class="footer">&copy; 2025 Zenenation. All rights reserved.</div>
            </div>
            </body></html>
            """.formatted(name, couponCode, expiryDays);
    }

}