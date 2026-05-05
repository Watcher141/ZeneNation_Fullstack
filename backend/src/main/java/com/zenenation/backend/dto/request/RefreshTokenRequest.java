package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent by frontend when the access token expires.
 *
 * Flow:
 * 1. Access token expires (after 1 day)
 * 2. Frontend gets 401 Unauthorized on any API call
 * 3. Frontend sends refresh token to /api/v1/auth/refresh
 * 4. We validate refresh token → issue new access token + new refresh token
 * 5. Frontend stores new tokens and retries the original request
 *
 * If refresh token is also expired → user must log in again.
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
