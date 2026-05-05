package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.ChangePasswordRequest;
import com.zenenation.backend.dto.request.UpdateProfileRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.UserProfileResponse;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.OAuthProvider;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.repository.UserRepository;
import com.zenenation.backend.service.CloudinaryService;
import com.zenenation.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Profile", description = "View and update profile, change password")
public class UserController {

    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    @Value("${cloudinary.folders.categories}")
    private String profileFolder;

    /**
     * GET /api/v1/user/profile
     * Get current logged-in user's profile.
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        User user = securityUtil.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", toProfileResponse(user)));
    }

    /**
     * PUT /api/v1/user/profile
     * Update name and/or phone number.
     */
    @PutMapping("/profile")
    @Operation(summary = "Update profile name and phone")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = securityUtil.getCurrentUser();

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", toProfileResponse(user)));
    }

    /**
     * POST /api/v1/user/profile/image
     * Upload or replace profile picture.
     */
    @PostMapping("/profile/image")
    @Operation(summary = "Upload profile picture")
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadProfileImage(
            @RequestParam("image") MultipartFile image) {

        User user = securityUtil.getCurrentUser();

        Map<String, String> result;
        if (user.getProfileImageUrl() != null && user.getProvider() == OAuthProvider.LOCAL) {
            // Only replace if it's not a Google-provided image
            result = cloudinaryService.uploadImage(image, "ecommerce/profiles");
        } else {
            result = cloudinaryService.uploadImage(image, "ecommerce/profiles");
        }

        user.setProfileImageUrl(result.get("url"));
        user = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Profile image updated", toProfileResponse(user)));
    }

    /**
     * PUT /api/v1/user/password
     * Change password — LOCAL users only.
     */
    @PutMapping("/password")
    @Operation(summary = "Change password (LOCAL accounts only)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        User user = securityUtil.getCurrentUser();

        // OAuth2 users have no password
        if (user.getProvider() != OAuthProvider.LOCAL) {
            throw new BadRequestException(
                    "Password change is not available for Google login accounts"
            );
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Confirm new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        // Prevent reuse of same password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password cannot be the same as current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .provider(user.getProvider())
                .profileImageUrl(user.getProfileImageUrl())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
