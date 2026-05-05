package com.zenenation.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist in the database.
 * Maps to HTTP 404 Not Found.
 *
 * Usage examples:
 *   throw new ResourceNotFoundException("Product", "id", 42L);
 *   → "Product not found with id: 42"
 *
 *   throw new ResourceNotFoundException("Category", "name", "Electronics");
 *   → "Category not found with name: Electronics"
 *
 *   throw new ResourceNotFoundException("User not found");
 *   → custom message directly
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    /**
     * Convenience constructor that builds a standard message.
     * @param resourceName  the entity name  (e.g. "Product")
     * @param fieldName     the field searched (e.g. "id")
     * @param fieldValue    the value searched (e.g. 42)
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
            String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue),
            HttpStatus.NOT_FOUND
        );
    }
}
