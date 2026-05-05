package com.zenenation.backend.util;

import com.zenenation.backend.entity.User;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.exception.UnauthorizedException;
import com.zenenation.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper to get the currently authenticated user in any service.
 *
 * WHY IS THIS NEEDED?
 * After JwtAuthFilter runs, Spring Security stores the authenticated
 * user's email in the SecurityContext. But we need the full User entity
 * (with ID, role, etc.) — not just the email string.
 *
 * Instead of writing this DB lookup in every service method,
 * we centralize it here. Every service just calls:
 *
 *   User currentUser = securityUtil.getCurrentUser();
 *
 * That's it — full User entity, ready to use.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    /**
     * Get the currently authenticated user's email from SecurityContext.
     *
     * @return  email string of the logged-in user
     * @throws  UnauthorizedException if no authentication found
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("No authenticated user found");
        }

        return authentication.getName(); // Returns the email (our username)
    }

    /**
     * Get the full User entity for the currently authenticated user.
     * Makes one DB call — use wisely (don't call in a loop).
     *
     * @return  full User entity from database
     * @throws  UnauthorizedException if not logged in
     * @throws  ResourceNotFoundException if user was deleted after token was issued
     */
    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found in database: " + email
                ));
    }

    /**
     * Get the ID of the currently authenticated user.
     * Slightly more efficient than getCurrentUser() when you only need the ID.
     *
     * @return  user ID (Long)
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Check if the current user has admin role.
     * Used in services where both admin and user can access an endpoint
     * but with different data visibility.
     *
     * Example: Admin sees all orders, user sees only their own orders.
     *
     * @return true if current user is ROLE_ADMIN
     */
    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
