package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.ForgotPasswordRequest;
import com.zenenation.backend.dto.request.ResetPasswordRequest;

public interface PasswordResetService {

    /**
     * Step 1 — User requests reset.
     * Generates a secure token, stores it, sends reset email.
     *
     * IMPORTANT: Always returns success message even if email
     * doesn't exist — prevents user enumeration attacks.
     * (Attacker can't know if an email is registered or not)
     */
    void requestPasswordReset(ForgotPasswordRequest request);

    /**
     * Step 2 — User submits new password with token.
     * Validates token → updates password → marks token used.
     */
    void resetPassword(ResetPasswordRequest request);
}
