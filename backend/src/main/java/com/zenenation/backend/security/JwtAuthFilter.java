package com.zenenation.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — runs ONCE per HTTP request.
 *
 * Extends OncePerRequestFilter which guarantees exactly one execution
 * per request, even in complex filter chains. (Important for correctness.)
 *
 * WHAT IT DOES on every request:
 * 1. Look for "Authorization: Bearer <token>" header
 * 2. If missing or malformed → skip (request continues as anonymous)
 * 3. If present → extract email from token
 * 4. Load user from DB by email
 * 5. Validate token against user
 * 6. If valid → set authentication in SecurityContext
 * 7. Spring Security then allows/blocks based on the route's access rules
 *
 * If at any step something is wrong, we simply don't set authentication.
 * Spring Security will then treat the request as unauthenticated and
 * return 401 for protected routes automatically.
 *
 * We NEVER throw exceptions here — the filter must always
 * call filterChain.doFilter() to pass control to the next filter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // ------------------------------------------------------------------
        // STEP 1: Extract Authorization header
        // ------------------------------------------------------------------
        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header or it doesn't start with "Bearer ",
        // skip JWT processing entirely — request continues unauthenticated.
        // Public routes (product listing, category browsing) will still work.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ------------------------------------------------------------------
        // STEP 2: Extract token from header
        // Header format: "Bearer eyJhbGciOiJIUzI1NiJ9...."
        // We strip the "Bearer " prefix (7 characters)
        // ------------------------------------------------------------------
        final String jwt = authHeader.substring(7);

        // ------------------------------------------------------------------
        // STEP 3: Extract email from token
        // ------------------------------------------------------------------
        final String userEmail;
        try {
            userEmail = jwtUtil.extractEmail(jwt);
        } catch (Exception e) {
            // Token is malformed, expired, or tampered with
            // Log at debug — this is normal for expired tokens
            log.debug("Could not extract email from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ------------------------------------------------------------------
        // STEP 4: Authenticate — only if we have an email AND
        // the SecurityContext doesn't already have authentication
        // (avoids redundant DB calls for already-authenticated requests)
        // ------------------------------------------------------------------
        if (userEmail != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load full user from DB
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(userEmail);
            } catch (Exception e) {
                log.debug("User not found for JWT email: {}", userEmail);
                filterChain.doFilter(request, response);
                return;
            }

            // ------------------------------------------------------------------
            // STEP 5: Validate token against loaded user
            // Checks: signature, expiry, token type = "access"
            // ------------------------------------------------------------------
            if (jwtUtil.isAccessTokenValid(jwt, userDetails)) {

                // ------------------------------------------------------------------
                // STEP 6: Create authentication token and set in SecurityContext
                //
                // UsernamePasswordAuthenticationToken with 3 args = authenticated
                // UsernamePasswordAuthenticationToken with 2 args = not yet authenticated
                // We use 3 args here → tells Spring Security this user IS authenticated
                // ------------------------------------------------------------------
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                           // credentials null — no password needed here
                                userDetails.getAuthorities()    // ROLE_USER or ROLE_ADMIN
                        );

                // Attach request details (IP address, session ID) to the auth token
                // Used by Spring Security for audit logging
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set authentication in the SecurityContext
                // From this point, Spring Security knows who this user is
                // and what roles they have for this request
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT authentication set for user: {}", userEmail);
            } else {
                log.debug("JWT validation failed for user: {}", userEmail);
            }
        }

        // ------------------------------------------------------------------
        // STEP 7: Always pass control to the next filter in the chain
        // ------------------------------------------------------------------
        filterChain.doFilter(request, response);
    }
}
