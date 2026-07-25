package com.zenenation.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthEntryPoint)
            )
            .authorizeHttpRequests(auth -> auth

                // ── PUBLIC ────────────────────────────────────────────────────
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/api-docs/**",   "/api-docs.yaml"
                ).permitAll()
                // H-04: Only the health endpoint is public — lets load balancers
                // and uptime monitors check liveness without exposing internals.
                // All other actuator endpoints (metrics, env, loggers, etc.)
                // require ADMIN so they are not reachable by anonymous callers.
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/ping").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/announcements/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/home-sections/active").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/shipping/config").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/fbt-sets/for-product").permitAll()

                // ── ADMIN ─────────────────────────────────────────────────────
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/shipping/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,   "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/products/**").hasRole("ADMIN")

                // ── AUTHENTICATED ─────────────────────────────────────────────
                .requestMatchers("/api/v1/cart/**").authenticated()
                .requestMatchers("/api/v1/orders/**").authenticated()
                .requestMatchers("/api/v1/address/**").authenticated()
                .requestMatchers("/api/v1/payments/**").authenticated()
                .requestMatchers("/api/v1/coupons/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/coupons/**").authenticated()
                .requestMatchers("/api/v1/user/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2SuccessHandler)
                .failureUrl("/api/v1/auth/oauth2/failure")
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // H-05: Use an explicit allowlist instead of wildcard subdomain patterns.
        // "https://*.vercel.app" matches ANY Vercel deployment (including attackers').
        // All legitimate frontend origins must be declared explicitly here or
        // provided via the app.cors.allowed-origins property.
        List<String> origins = new ArrayList<>();
        origins.add("https://zenenation.in");
        origins.add("https://www.zenenation.in");
        origins.add("http://localhost:3000");
        origins.add("http://localhost:5173");
        if (corsAllowedOrigins != null) {
            // Extra origins from application.yml / env (e.g. FRONTEND_URL in Render)
            origins.addAll(corsAllowedOrigins);
        }
        config.setAllowedOriginPatterns(origins);

        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept",
            "X-Requested-With", "Origin"
        ));

        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}