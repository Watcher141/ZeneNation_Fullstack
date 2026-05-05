package com.zenenation.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all custom exceptions in this application.
 *
 * Every custom exception extends this class.
 * It carries an HttpStatus so the GlobalExceptionHandler
 * knows exactly which HTTP status code to return — no guessing.
 *
 * Why extend RuntimeException and not Exception?
 * RuntimeException = unchecked. We don't have to declare it
 * in every method signature with "throws". Much cleaner code.
 */
@Getter
public class BaseException extends RuntimeException {

    private final HttpStatus status;

    public BaseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
