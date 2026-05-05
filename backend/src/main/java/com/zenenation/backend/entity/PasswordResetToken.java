package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores a temporary token for password reset.
 *
 * FLOW:
 * 1. User requests reset → we generate a UUID token → store here → email the link
 * 2. User clicks link → frontend sends token to our API
 * 3. We verify: token exists + not expired + not used
 * 4. If valid → update password → mark token as used
 *
 * Token expires in 15 minutes — short window reduces attack surface.
 * Token is single use — prevents replay attacks.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this token is still valid.
     * Valid = not used AND not expired.
     */
    public boolean isValid() {
        return !isUsed && LocalDateTime.now().isBefore(expiresAt);
    }
}
