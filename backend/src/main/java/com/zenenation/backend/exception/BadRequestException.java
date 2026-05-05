package com.zenenation.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the request is logically invalid — not a validation error,
 * but a business rule violation.
 * Maps to HTTP 400 Bad Request.
 *
 * Usage examples:
 *   throw new BadRequestException("COD is not available for orders above ₹10,000");
 *   throw new BadRequestException("Cannot cancel an order that is already shipped");
 *   throw new BadRequestException("New password cannot be the same as current password");
 *   throw new BadRequestException("Password confirmation does not match");
 */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
