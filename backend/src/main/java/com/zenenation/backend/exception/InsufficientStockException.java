package com.zenenation.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user tries to order more units than available in stock.
 * Maps to HTTP 400 Bad Request.
 *
 * Usage examples:
 *   throw new InsufficientStockException("iPhone 15", 3, 1);
 *   → "Insufficient stock for iPhone 15. Requested: 3, Available: 1"
 *
 *   throw new InsufficientStockException("Product is out of stock");
 */
public class InsufficientStockException extends BaseException {

    public InsufficientStockException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Convenience constructor with product name and quantities.
     * @param productName   name of the product
     * @param requested     quantity user is trying to order
     * @param available     actual stock available
     */
    public InsufficientStockException(String productName, int requested, int available) {
        super(
            String.format(
                "Insufficient stock for %s. Requested: %d, Available: %d",
                productName, requested, available
            ),
            HttpStatus.BAD_REQUEST
        );
    }
}
