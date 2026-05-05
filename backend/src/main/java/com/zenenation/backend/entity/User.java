package com.zenenation.backend.entity;

import com.zenenation.backend.enums.OAuthProvider;
import com.zenenation.backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the system.
 *
 * A user can be:
 *  - Registered manually (OAuthProvider.LOCAL) with email + password
 *  - Registered via Google OAuth2 (OAuthProvider.GOOGLE) — no password stored
 *
 * One user can have:
 *  - Multiple addresses
 *  - One active cart
 *  - Multiple orders
 */
@Entity
@Table(
    name = "users",
    indexes = {
        // Fast lookup by email (used on every login)
        @Index(name = "idx_users_email", columnList = "email"),
        // Fast lookup by provider (used in OAuth2 flow)
        @Index(name = "idx_users_provider", columnList = "provider")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // BASIC INFO
    // ------------------------------------------------------------------

    @Column(nullable = false)
    private String name;

    /**
     * Email is unique across all users.
     * Used as the primary login identifier.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Stored as BCrypt hash — NEVER plain text.
     * NULL for OAuth2 users (they have no password).
     */
    @Column(nullable = true)
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    // ------------------------------------------------------------------
    // ROLE & AUTH PROVIDER
    // ------------------------------------------------------------------

    /**
     * Stored as STRING in DB ("ROLE_USER" / "ROLE_ADMIN").
     * Default is ROLE_USER for all new registrations.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    /**
     * How this account was created.
     * LOCAL = email+password, GOOGLE = OAuth2
     * Stored as STRING so adding new providers later is safe.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OAuthProvider provider = OAuthProvider.LOCAL;

    /**
     * The unique ID returned by Google for this user.
     * NULL for LOCAL users.
     * Used to match returning OAuth2 users without relying on email alone.
     */
    @Column(name = "provider_id")
    private String providerId;

    // ------------------------------------------------------------------
    // ACCOUNT STATE
    // ------------------------------------------------------------------

    /**
     * Whether this account is active.
     * Admin can deactivate a user without deleting their order history.
     * Inactive users cannot log in.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Whether email has been verified.
     * OAuth2 users are auto-verified (Google already verified their email).
     * LOCAL users need to verify via email link (future feature slot).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    // ------------------------------------------------------------------
    // PROFILE IMAGE
    // ------------------------------------------------------------------

    /**
     * Cloudinary URL for profile picture.
     * For OAuth users, this is pre-filled from Google profile.
     * NULL if user hasn't uploaded one.
     */
    @Column(name = "profile_image_url")
    private String profileImageUrl;

    // ------------------------------------------------------------------
    // RELATIONSHIPS
    // ------------------------------------------------------------------

    /**
     * User's saved addresses.
     * Loaded lazily — only fetched when explicitly accessed.
     * CascadeType.ALL — deleting user deletes all their addresses.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Address> addresses = new ArrayList<>();

    // ------------------------------------------------------------------
    // AUDIT FIELDS
    // ------------------------------------------------------------------

    /**
     * Automatically set to current timestamp when record is first created.
     * Never changes after that — updatable = false enforces this.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically updated to current timestamp on every save.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
