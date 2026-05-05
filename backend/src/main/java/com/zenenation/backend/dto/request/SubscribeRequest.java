package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SubscribeRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    private String name;
}