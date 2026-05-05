package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.ForgotPasswordRequest;
import com.zenenation.backend.dto.request.LoginRequest;
import com.zenenation.backend.dto.request.RefreshTokenRequest;
import com.zenenation.backend.dto.request.RegisterRequest;
import com.zenenation.backend.dto.request.ResetPasswordRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.AuthResponse;
import com.zenenation.backend.service.AuthService;
import com.zenenation.backend.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, forgot password")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * POST /api/v1/auth/forgot-password
     * Send reset link to email. Always returns success (security — no user enumeration).
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists with this email, a reset link has been sent."
        ));
    }

    /**
     * POST /api/v1/auth/reset-password
     * Submit new password using token from email link.
     */
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token from email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset successfully. Please login with your new password."
        ));
    }

    @GetMapping("/oauth2/failure")
    @Operation(summary = "OAuth2 login failure handler")
    public ResponseEntity<ApiResponse<Void>> oauth2Failure() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Google login failed. Please try again."));
    }
}
