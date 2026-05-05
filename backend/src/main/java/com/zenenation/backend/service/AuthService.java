package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.LoginRequest;
import com.zenenation.backend.dto.request.RefreshTokenRequest;
import com.zenenation.backend.dto.request.RegisterRequest;
import com.zenenation.backend.dto.response.AuthResponse;

/**
 * Contract for all authentication operations.
 *
 * Covers:
 * - LOCAL registration (email + password)
 * - LOCAL login (email + password → JWT)
 * - Token refresh (refresh token → new access + refresh tokens)
 *
 * OAuth2 (Google login) is handled separately by:
 * - Spring Security OAuth2 filter chain
 * - OAuth2SuccessHandler (creates/updates user, issues JWT)
 * No service method needed for OAuth2 — Spring handles the flow.
 */
public interface AuthService {

    /**
     * Register a new user with email and password.
     * Creates a User entity + an empty Cart for them.
     * Returns JWT tokens so user is immediately logged in after registering.
     *
     * @throws DuplicateResourceException if email already exists
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Login with email and password.
     * Verifies credentials, returns JWT access + refresh tokens.
     *
     * @throws BadCredentialsException  if email/password is wrong
     * @throws DisabledException        if account is deactivated
     */
    AuthResponse login(LoginRequest request);

    /**
     * Exchange a valid refresh token for a new access token + new refresh token.
     * Both tokens are rotated on every refresh (refresh token rotation).
     *
     * WHY ROTATE REFRESH TOKENS?
     * If a refresh token is stolen, using it once invalidates it on the attacker's
     * next use — because the real user will have already rotated it.
     * (Fully stateless rotation — no DB blacklist needed for basic protection.)
     *
     * @throws UnauthorizedException if refresh token is invalid or expired
     */
    AuthResponse refreshToken(RefreshTokenRequest request);
}
