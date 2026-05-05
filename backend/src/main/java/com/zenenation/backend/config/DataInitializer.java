package com.zenenation.backend.config;

import com.zenenation.backend.entity.Cart;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.OAuthProvider;
import com.zenenation.backend.enums.Role;
import com.zenenation.backend.repository.CartRepository;
import com.zenenation.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs ONCE on every application startup (after Spring context loads).
 *
 * Responsibilities:
 * 1. Create the admin account if it doesn't exist yet
 *    (first time the app starts on a fresh database)
 *
 * CommandLineRunner — Spring Boot calls run() after the app is fully started.
 * Safe to use — if admin already exists, it does nothing.
 *
 * Admin credentials come from application.yml (app.admin.*)
 * which reads from environment variables in production.
 * CHANGE THE DEFAULT PASSWORD via environment variable before going live.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(String... args) {
        createAdminIfNotExists();
    }

    /**
     * Creates the admin account on first startup.
     * Does nothing if admin already exists — safe to run on every restart.
     */
    private void createAdminIfNotExists() {
        String adminEmail = appProperties.getAdmin().getEmail();

        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists: {}", adminEmail);
            return;
        }

        log.info("Creating admin account: {}", adminEmail);

        // Create admin user
        User admin = User.builder()
                .name("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(appProperties.getAdmin().getPassword()))
                .role(Role.ROLE_ADMIN)
                .provider(OAuthProvider.LOCAL)
                .isActive(true)
                .isEmailVerified(true)
                .build();

        admin = userRepository.save(admin);

        // Every user gets a cart — including admin (in case they want to test)
        Cart adminCart = Cart.builder()
                .user(admin)
                .build();

        cartRepository.save(adminCart);

        log.info("Admin account created successfully: {}", adminEmail);
        log.warn("IMPORTANT: Change the default admin password via ADMIN_PASSWORD environment variable before going live!");
    }
}
