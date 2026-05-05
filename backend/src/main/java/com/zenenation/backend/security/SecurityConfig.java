package com.zenenation.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Main Spring Security configuration.
 *
 * KEY DECISIONS:
 *
 * 1. STATELESS sessions — we use JWT, not server-side sessions.
 *    SessionCreationPolicy.STATELESS tells Spring never to create
 *    an HttpSession. Every request must carry its own JWT.
 *
 * 2. CSRF disabled — safe for stateless JWT APIs.
 *    CSRF attacks only work against session-based auth (cookies).
 *    Since we use Bearer tokens in headers, CSRF is not a threat here.
 *
 * 3. JwtAuthFilter runs BEFORE UsernamePasswordAuthenticationFilter.
 *    This ensures every request is checked for a JWT before
 *    Spring Security does anything else.
 *
 * 4. @EnableMethodSecurity — enables @PreAuthorize on individual
 *    service or controller methods for fine-grained access control.
 *    Example: @PreAuthorize("hasRole('ADMIN')") on admin endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // =========================================================================
    // SECURITY FILTER CHAIN
    // =========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ------------------------------------------------------------------
            // CSRF — disabled for stateless JWT API (safe, see class javadoc)
            // ------------------------------------------------------------------
            .csrf(AbstractHttpConfigurer::disable)

            // ------------------------------------------------------------------
            // CORS — use our custom config bean defined below
            // ------------------------------------------------------------------
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ------------------------------------------------------------------
            // SESSION — stateless, no HttpSession ever created
            // ------------------------------------------------------------------
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ------------------------------------------------------------------
            // AUTH ENTRY POINT — our custom 401 JSON response
            // ------------------------------------------------------------------
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthEntryPoint)
            )

            // ------------------------------------------------------------------
            // ROUTE ACCESS RULES
            // Rules are evaluated TOP TO BOTTOM — first match wins.
            // Put most specific rules first, most general last.
            // ------------------------------------------------------------------
            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC routes — no login needed ──────────────────────────

                // Auth endpoints — register, login, refresh, OAuth2 callback
                .requestMatchers("/api/v1/auth/**").permitAll()

                // OAuth2 endpoints — Google login redirect handling
                .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()

                // Public product browsing — anyone can see products and categories
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()

                // Swagger UI and OpenAPI docs — public in dev, disabled in prod
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/api-docs.yaml"
                ).permitAll()

                // Spring Actuator health endpoint — public
                .requestMatchers("/actuator/health").permitAll()

                // ── ADMIN only routes ─────────────────────────────────────────

                // All admin dashboard and management endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Admin can create, update, delete categories
                .requestMatchers(HttpMethod.POST, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")

                // Admin can create, update, delete products and manage images
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/**").hasRole("ADMIN")

                // ── AUTHENTICATED USER routes ──────────────────────────────────

                // Cart, orders, addresses, profile — require login
                .requestMatchers("/api/v1/cart/**").authenticated()
                .requestMatchers("/api/v1/orders/**").authenticated()
                .requestMatchers("/api/v1/address/**").authenticated()
                .requestMatchers("/api/v1/payments/**").authenticated()
                .requestMatchers("/api/v1/coupons/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/coupons/**").authenticated()
                .requestMatchers("/api/v1/user/**").authenticated()

                // Everything else — require authentication by default
                // This is the safe default — new routes are protected until explicitly opened
                .anyRequest().authenticated()
            )

            // ------------------------------------------------------------------
            // OAUTH2 LOGIN — Google login configuration
            // ------------------------------------------------------------------
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)
                // On OAuth2 failure, redirect to frontend with error param
                .failureUrl("/api/v1/auth/oauth2/failure")
            )

            // ------------------------------------------------------------------
            // AUTHENTICATION PROVIDER — how we verify email+password logins
            // ------------------------------------------------------------------
            .authenticationProvider(authenticationProvider())

            // ------------------------------------------------------------------
            // JWT FILTER — runs before Spring's built-in auth filter
            // ------------------------------------------------------------------
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // =========================================================================
    // AUTHENTICATION PROVIDER
    // Tells Spring Security: use our UserDetailsService + BCrypt to verify logins
    // =========================================================================

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // =========================================================================
    // AUTHENTICATION MANAGER
    // Used in AuthService to programmatically authenticate email+password
    // =========================================================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // =========================================================================
    // PASSWORD ENCODER
    // BCrypt with strength 12 — strong enough for production, fast enough for dev
    // Strength 10 = ~100ms per hash. Strength 12 = ~400ms. Sweet spot.
    // =========================================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // =========================================================================
    // CORS CONFIGURATION
    // Controls which frontend origins can call our API
    // =========================================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins — read from application.yml in production via env var
        // For now hardcoded here; move to @ConfigurationProperties later
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",    // React/Next.js dev
            "http://localhost:5173",    // Vite dev
            "${FRONTEND_URL:http://localhost:3000}"
        ));

        // Allowed HTTP methods
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allowed headers — Authorization carries JWT, Content-Type for JSON
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Origin"
        ));

        // Allow frontend to read the Authorization header in responses
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies/credentials in cross-origin requests
        config.setAllowCredentials(true);

        // Cache preflight (OPTIONS) response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Apply to all routes
        return source;
    }
}
