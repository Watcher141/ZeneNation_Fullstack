package com.zenenation.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user tries to perform an action they are not
 * authenticated for — i.e. no valid token or token expired.
 * Maps to HTTP 401 Unauthorized.
 *
 * Note: Do NOT confuse with AccessDeniedException (403).
 * 401 = not authenticated (who are you?)
 * 403 = authenticated but not allowed (I know who you are, but you can't do this)
 *
 * Usage examples:
 *   throw new UnauthorizedException("Invalid or expired token");
 *   throw new UnauthorizedException("Please login to continue");
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
