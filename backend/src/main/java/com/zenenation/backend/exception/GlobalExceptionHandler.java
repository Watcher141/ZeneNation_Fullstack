package com.zenenation.backend.exception;

import com.zenenation.backend.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler — catches ALL exceptions thrown anywhere in the app.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * Every handler method here returns JSON automatically.
 *
 * HOW IT WORKS:
 * When any exception is thrown in a controller or service,
 * Spring looks for a matching @ExceptionHandler here.
 * If found → runs that handler → returns consistent ApiResponse JSON.
 * If not found → falls through to the generic Exception handler at the bottom.
 *
 * This means:
 * - Frontend ALWAYS gets ApiResponse JSON (never Spring's default error HTML)
 * - Every error has the correct HTTP status code
 * - Validation errors list exactly which fields failed and why
 * - No sensitive stack traces leak to the client
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================================================================
    // 1. OUR CUSTOM EXCEPTIONS
    // =========================================================================

    /**
     * Handles all exceptions that extend BaseException.
     * Since all our custom exceptions extend BaseException,
     * this one handler covers ResourceNotFoundException,
     * BadRequestException, DuplicateResourceException etc.
     * Each carries its own HttpStatus — we just use it directly.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        log.warn("Business exception [{}]: {}", ex.getStatus(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage()));
    }

    // =========================================================================
    // 2. SPRING VALIDATION EXCEPTIONS
    // =========================================================================

    /**
     * Handles @Valid / @Validated failures on request DTOs.
     * Returns a map of { fieldName: "error message" } so the
     * frontend can highlight exactly which fields are wrong.
     *
     * Example response:
     * {
     *   "success": false,
     *   "message": "Validation failed",
     *   "data": {
     *     "email": "Please provide a valid email address",
     *     "password": "Password must have at least one uppercase..."
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        // Collect all field errors into the map
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    /**
     * Handles missing required @RequestParam in URL.
     * Example: /api/v1/products?page=1 (missing required 'size' param)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {

        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        log.warn("Missing request parameter: {}", ex.getParameterName());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * Handles type mismatch in path variables or request params.
     * Example: /api/v1/products/abc (id should be a number, not "abc")
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );
        log.warn("Type mismatch: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // 3. SPRING SECURITY EXCEPTIONS
    // =========================================================================

    /**
     * Handles wrong email/password on login.
     * Spring Security throws this when credentials don't match.
     * We return a vague message intentionally — never confirm
     * whether the email exists or not (security best practice).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
    }

    /**
     * Handles access to admin endpoints by non-admin users.
     * Spring Security throws AccessDeniedException when
     * @PreAuthorize("hasRole('ADMIN')") fails.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("You do not have permission to perform this action"));
    }

    /**
     * Handles login attempt on a deactivated account.
     * Thrown when isActive = false and user tries to log in.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledAccount(DisabledException ex) {
        log.warn("Disabled account login attempt");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Your account has been deactivated. Please contact support."));
    }

    /**
     * Handles locked account login attempts.
     * Future use: lock account after N failed login attempts.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedAccount(LockedException ex) {
        log.warn("Locked account login attempt");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Your account has been locked. Please contact support."));
    }

    // =========================================================================
    // 4. FILE UPLOAD EXCEPTIONS
    // =========================================================================

    /**
     * Handles file uploads exceeding the max size configured in application.yml.
     * spring.servlet.multipart.max-file-size = 10MB
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex) {

        log.warn("File upload size exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("File size exceeds the maximum allowed limit of 10MB"));
    }

    // =========================================================================
    // 5. CATCH-ALL — any unhandled exception
    // =========================================================================

    /**
     * Safety net — catches anything not handled above.
     *
     * IMPORTANT: We log the full stack trace here (log.error)
     * but return a GENERIC message to the client.
     * Never leak internal error details or stack traces to frontend.
     *
     * These should be investigated — every unexpected exception
     * here is a bug or missing handler that needs fixing.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex); // Full stack trace in logs
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Something went wrong. Please try again later."));
    }
}
