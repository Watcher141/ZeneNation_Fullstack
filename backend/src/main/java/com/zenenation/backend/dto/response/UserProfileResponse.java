package com.zenenation.backend.dto.response;

import com.zenenation.backend.enums.OAuthProvider;
import com.zenenation.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Returned when user fetches their own profile.
 * Also used by admin when viewing a user's details.
 * Password is NEVER included in any response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private Role role;
    private OAuthProvider provider;
    private String profileImageUrl;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
