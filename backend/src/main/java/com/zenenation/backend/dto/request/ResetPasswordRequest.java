package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * User provides the reset token (from email link) + their new password.
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

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
