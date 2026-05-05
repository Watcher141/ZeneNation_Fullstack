package com.zenenation.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when trying to create a resource that already exists.
 * Maps to HTTP 409 Conflict.
 *
 * Usage examples:
 *   throw new DuplicateResourceException("User already exists with email: user@gmail.com");
 *   throw new DuplicateResourceException("Category already exists with name: Electronics");
 */
public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
