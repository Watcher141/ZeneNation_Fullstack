package com.zenenation.backend.repository;

import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 *
 * Spring Data JPA auto-implements all methods at runtime.
 * We only define custom queries here — standard CRUD (save, findById,
 * findAll, delete) is inherited from JpaRepository for free.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email.
     * Used on every login attempt (LOCAL + OAuth2).
     * Returns Optional — forces caller to handle "user not found" case.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered.
     * Used during registration to prevent duplicates.
     * More efficient than findByEmail() — only checks existence, no data fetch.
     */
    boolean existsByEmail(String email);

    /**
     * Find user by OAuth2 provider + provider's user ID.
     * Used in OAuth2 flow to match returning Google users.
     * Example: findByProviderAndProviderId(GOOGLE, "google-user-id-12345")
     */
    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
