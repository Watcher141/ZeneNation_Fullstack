package com.zenenation.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Universal API response envelope.
 *
 * EVERY endpoint in this system returns this wrapper.
 * This gives the frontend a consistent structure to always expect:
 *
 * Success:
 * {
 *   "success": true,
 *   "message": "Product created successfully",
 *   "data": { ...actual payload... },
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * Error:
 * {
 *   "success": false,
 *   "message": "Product not found",
 *   "data": null,
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * @param <T> the type of the data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in JSON output
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Static factory methods for cleaner controller code ──

    /**
     * Quick success response with data.
     * Usage: ApiResponse.success("Product created", product)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Success response with no data payload (e.g. delete operations).
     * Usage: ApiResponse.success("Product deleted successfully")
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Error response.
     * Usage: ApiResponse.error("Product not found")
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
