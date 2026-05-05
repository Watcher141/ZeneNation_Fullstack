package com.zenenation.backend.repository;

import com.zenenation.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** Find token by its value — used during verification */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Invalidate all existing unused tokens for an email.
     * Called before issuing a new token — prevents multiple
     * active reset links for the same user at the same time.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.isUsed = true WHERE t.email = :email AND t.isUsed = false")
    void invalidateAllTokensForEmail(@Param("email") String email);

    /**
     * Delete all expired or used tokens for cleanup.
     * Can be called periodically to keep the table clean.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.isUsed = true OR t.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredAndUsedTokens();
}
