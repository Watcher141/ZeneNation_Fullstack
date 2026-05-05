package com.zenenation.backend.security;

import com.zenenation.backend.entity.User;
import com.zenenation.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tells Spring Security HOW to load a user from our database.
 *
 * Spring Security doesn't know about our User entity or UserRepository.
 * This bridge class implements UserDetailsService so Spring Security
 * can call loadUserByUsername(email) whenever it needs to verify a user.
 *
 * Called in two scenarios:
 * 1. During JWT filter — to load user and verify the token against them
 * 2. During login — to load user and verify their password
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their email address.
     *
     * Spring Security uses "username" as the login identifier —
     * in our system that identifier is the email.
     *
     * Returns a Spring Security UserDetails object that contains:
     * - username (email)
     * - password (BCrypt hash — Spring verifies this automatically)
     * - authorities (roles — ROLE_USER or ROLE_ADMIN)
     * - account flags (enabled, locked etc.)
     *
     * @Transactional — needed because we access user data
     * within a session that may be outside a transaction.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    // We use UsernameNotFoundException here (Spring Security standard)
                    // GlobalExceptionHandler converts it to BadCredentialsException
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        // Convert our Role enum to Spring Security's GrantedAuthority
        // Spring Security expects roles as strings like "ROLE_USER", "ROLE_ADMIN"
        // Our enum already stores them this way, so .name() works perfectly
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().name())
        );

        // Build Spring Security's UserDetails from our User entity
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                // isActive controls whether this account can log in
                .disabled(!user.getIsActive())
                // Account never expires in our system (can add expiry later)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(authorities)
                .build();
    }
}
