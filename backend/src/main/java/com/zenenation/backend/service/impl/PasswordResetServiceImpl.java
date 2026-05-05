package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.request.ForgotPasswordRequest;
import com.zenenation.backend.dto.request.ResetPasswordRequest;
import com.zenenation.backend.entity.PasswordResetToken;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.OAuthProvider;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.repository.PasswordResetTokenRepository;
import com.zenenation.backend.repository.UserRepository;
import com.zenenation.backend.service.EmailService;
import com.zenenation.backend.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FORGOT PASSWORD FLOW:
 *
 * Step 1 — requestPasswordReset():
 *   1. Check if email exists in DB
 *   2. If exists AND is LOCAL account (not Google):
 *      a. Invalidate any existing unused tokens for this email
 *      b. Generate a secure UUID token
 *      c. Store token with 15-minute expiry
 *      d. Send email with reset link
 *   3. Always return success (even if email not found — security)
 *
 * Step 2 — resetPassword():
 *   1. Find token in DB
 *   2. Validate: exists + not used + not expired
 *   3. Check new password != confirm password match
 *   4. Update user's password (BCrypt hash)
 *   5. Mark token as used (prevents reuse)
 *   6. Clean up all other tokens for this email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Token expiry: 15 minutes
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String frontendBaseUrl;

    // -------------------------------------------------------------------------
    // STEP 1 — REQUEST RESET
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Look up user silently — don't reveal if email exists
        userRepository.findByEmail(email).ifPresent(user -> {
            // Only LOCAL accounts have passwords — OAuth2 users can't reset
            if (user.getProvider() != OAuthProvider.LOCAL) {
                log.info("Password reset requested for OAuth2 account — skipping: {}", email);
                return; // Silently skip — don't tell user why
            }

            if (!user.getIsActive()) {
                log.info("Password reset requested for inactive account — skipping: {}", email);
                return; // Silently skip deactivated accounts
            }

            // Invalidate any existing unused tokens for this email
            tokenRepository.invalidateAllTokensForEmail(email);

            // Generate secure unique token
            String token = UUID.randomUUID().toString();

            // Save token with expiry
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .email(email)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                    .isUsed(false)
                    .build();

            tokenRepository.save(resetToken);

            // Build reset link — points to frontend reset password page
            // Frontend reads the token from URL and calls our reset API
            String frontendBase = frontendBaseUrl
                    .replace("/oauth2/callback", "");
            String resetLink = frontendBase + "/reset-password?token=" + token;

            // Send email asynchronously (non-blocking)
            emailService.sendPasswordResetEmail(email, resetLink);

            log.info("Password reset token generated for: {}", email);
        });

        // Always log as if successful — no info leaked to caller
        log.info("Password reset requested for email: {} (response always success)", email);
    }

    // -------------------------------------------------------------------------
    // STEP 2 — RESET PASSWORD
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        // Find and validate token
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired reset link. Please request a new one."
                ));

        if (!resetToken.isValid()) {
            throw new BadRequestException(
                    "This reset link has expired or already been used. Please request a new one."
            );
        }

        // Find user
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Prevent reuse of same password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException(
                    "New password cannot be the same as your current password"
            );
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used — prevents replay attack
        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successfully for: {}", user.getEmail());
    }

    // -------------------------------------------------------------------------
    // SCHEDULED CLEANUP
    // -------------------------------------------------------------------------

    /**
     * Runs every hour — deletes expired and used tokens.
     * Keeps the password_reset_tokens table clean.
     * @Scheduled uses cron expression: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at :00
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredAndUsedTokens();
        log.debug("Expired password reset tokens cleaned up");
    }
}
