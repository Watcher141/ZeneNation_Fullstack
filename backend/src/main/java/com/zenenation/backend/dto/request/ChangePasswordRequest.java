package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Used when a logged-in user wants to change their password.
 * Only available for LOCAL provider users (not OAuth2 users).
 * currentPassword is required to prevent unauthorized changes
 * if someone gets access to an unlocked device.
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 32, message = "Password must be between 8 and 32 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Password must have at least one uppercase, one lowercase, one number, and one special character"
    )
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}
