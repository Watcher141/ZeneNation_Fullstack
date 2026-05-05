package com.zenenation.backend.dto.response;

import com.zenenation.backend.enums.OAuthProvider;
import com.zenenation.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned after successful login or registration.
 * Contains JWT tokens + basic user info so frontend
 * doesn't need a separate API call to get user details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    // JWT tokens
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    // Basic user info (avoids extra API call after login)
    private Long userId;
    private String name;
    private String email;
    private Role role;
    private OAuthProvider provider;
    private String profileImageUrl;
}
