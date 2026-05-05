package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single line item inside the cart response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productSlug;
    private String primaryImageUrl;
    private BigDecimal unitPrice;       // Current product price
    private BigDecimal discountedPrice; // After discount applied
    private Integer quantity;
    private BigDecimal totalPrice;      // discountedPrice × quantity
    private Integer availableStock;     // Frontend uses this to cap qty selector
    private Boolean isAvailable;        // False if product deleted or out of stock
    private Boolean isPreorder;         // True if product is a preorder item
}