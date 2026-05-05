package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.request.LoginRequest;
import com.zenenation.backend.dto.request.RefreshTokenRequest;
import com.zenenation.backend.dto.request.RegisterRequest;
import com.zenenation.backend.dto.response.AuthResponse;
import com.zenenation.backend.entity.Cart;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.OAuthProvider;
import com.zenenation.backend.enums.Role;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.DuplicateResourceException;
import com.zenenation.backend.exception.UnauthorizedException;
import com.zenenation.backend.repository.CartRepository;
import com.zenenation.backend.repository.UserRepository;
import com.zenenation.backend.security.CustomUserDetailsService;
import com.zenenation.backend.security.JwtUtil;
import com.zenenation.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration, login, and token refresh.
 *
 * REGISTRATION FLOW:
 * 1. Check email not already taken
 * 2. Hash password with BCrypt
 * 3. Save User entity
 * 4. Create empty Cart for the new user
 * 5. Generate JWT tokens
 * 6. Return AuthResponse (tokens + user info)
 *
 * LOGIN FLOW:
 * 1. Use Spring's AuthenticationManager to verify email + password
 *    (throws BadCredentialsException automatically if wrong)
 * 2. Load UserDetails
 * 3. Generate JWT tokens
 * 4. Return AuthResponse
 *
 * REFRESH FLOW:
 * 1. Validate refresh token signature and expiry
 * 2. Extract email from token
 * 3. Load user from DB
 * 4. Generate new access + refresh tokens (rotation)
 * 5. Return new AuthResponse
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final WelcomeCouponService welcomeCouponService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    // -------------------------------------------------------------------------
    // REGISTER
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Normalize email — always store lowercase
        String email = request.getEmail().trim().toLowerCase();

        // Check for duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "An account already exists with email: " + email
            );
        }

        // Build and save user
        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(Role.ROLE_USER)
                .provider(OAuthProvider.LOCAL)
                .isActive(true)
                .isEmailVerified(false) // Will be true after email verification (future)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", email);

        // Create empty cart for the new user
        // Every user has exactly one cart from the moment they register
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cartRepository.save(cart);

        // Generate welcome coupon for new user (async — won't block registration)
        try {
            welcomeCouponService.generateWelcomeCoupon(user);
        } catch (Exception e) {
            // Log but don't fail registration if coupon generation fails
            log.warn("Failed to generate welcome coupon for user {}: {}", email, e.getMessage());
        }

        // Generate tokens and return
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return buildAuthResponse(user, userDetails);
    }

    // -------------------------------------------------------------------------
    // LOGIN
    // -------------------------------------------------------------------------

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Spring's AuthenticationManager handles all verification:
        // - Loads user via CustomUserDetailsService
        // - Verifies password with BCrypt
        // - Checks isEnabled (isActive flag)
        // Throws BadCredentialsException, DisabledException etc. automatically
        // These are caught by GlobalExceptionHandler
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        // If we reach here — authentication succeeded
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        log.info("User logged in: {}", email);

        return buildAuthResponse(user, userDetails);
    }

    // -------------------------------------------------------------------------
    // REFRESH TOKEN
    // -------------------------------------------------------------------------

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Extract email from refresh token
        // jwtUtil throws UnauthorizedException if token is expired or invalid
        String email = jwtUtil.extractEmail(refreshToken);

        // Load user from DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Verify user is still active
        if (!user.getIsActive()) {
            throw new UnauthorizedException("Your account has been deactivated");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        // Validate refresh token type and expiry against this user
        if (!jwtUtil.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new UnauthorizedException("Invalid or expired refresh token. Please login again.");
        }

        log.info("Tokens refreshed for user: {}", email);

        // Issue brand new access + refresh tokens (rotation)
        return buildAuthResponse(user, userDetails);
    }

    // -------------------------------------------------------------------------
    // HELPER
    // -------------------------------------------------------------------------

    /**
     * Build the AuthResponse with fresh tokens and user info.
     * Used by register(), login(), and refreshToken().
     */
    private AuthResponse buildAuthResponse(User user, UserDetails userDetails) {
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}