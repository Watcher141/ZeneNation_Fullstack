package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Used for both CREATE and UPDATE product.
 * Images are handled via a separate dedicated upload endpoint.
 *
 * On UPDATE — all fields are optional individually,
 * but the service layer handles partial updates (PATCH behavior).
 */
@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private String name;

    @NotBlank(message = "Product description is required")
    private String description;

    @Size(max = 200, message = "Tagline cannot exceed 200 characters")
    private String tagline;

    private Boolean isPreorder = false;

    private java.time.LocalDate estimatedShipDate;

    @Size(max = 300)
    private String preorderNote;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100%")
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 0, message = "Weight cannot be negative")
    private Integer weightGrams = 0;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // isActive defaults to true on creation
    // Admin can set false to hide without deleting
    private Boolean isActive = true;
}