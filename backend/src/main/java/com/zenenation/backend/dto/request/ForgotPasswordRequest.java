package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * User provides their email to receive a reset link.
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
}
