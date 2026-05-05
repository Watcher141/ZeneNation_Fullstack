package com.zenenation.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user is authenticated but does not have
 * permission to perform the requested action.
 * Maps to HTTP 403 Forbidden.
 *
 * Usage examples:
 *   throw new ForbiddenException("You do not have permission to access this resource");
 *   throw new ForbiddenException("Only admins can perform this action");
 *   throw new ForbiddenException("You can only view your own orders");
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
