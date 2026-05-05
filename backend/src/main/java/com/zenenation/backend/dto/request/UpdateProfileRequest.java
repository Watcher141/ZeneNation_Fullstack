package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * User can update their own name, phone number, and profile image.
 * Email cannot be changed (it's the login identifier).
 * Password change is a separate dedicated endpoint.
 * All fields are optional — only provided fields are updated (PATCH behavior).
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please provide a valid Indian mobile number")
    private String phoneNumber;
}
