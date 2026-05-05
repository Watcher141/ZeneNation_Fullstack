package com.zenenation.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zenenation.backend.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Called by Spring Security when an unauthenticated request
 * tries to access a protected endpoint.
 *
 * WITHOUT this class:
 * Spring Security returns its default HTML error page or
 * a plain text "401 Unauthorized" — not our ApiResponse format.
 *
 * WITH this class:
 * Every 401 response is a proper JSON ApiResponse:
 * {
 *   "success": false,
 *   "message": "Authentication required. Please login to continue.",
 *   "timestamp": "..."
 * }
 *
 * This is triggered when:
 * - No Authorization header is present on a protected route
 * - JWT token is expired or invalid
 * - User is not logged in and tries to access /api/v1/cart, /api/v1/orders etc.
 */
@Component
@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthEntryPoint() {
        // Register JavaTimeModule so LocalDateTime serializes correctly
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        log.warn("Unauthorized access attempt to: {} | Reason: {}",
                request.getRequestURI(),
                authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);        // 401
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(
                "Authentication required. Please login to continue."
        );

        // Write ApiResponse JSON directly to the HTTP response body
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
