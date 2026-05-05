package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single product line inside an order response.
 * Uses snapshots — name and price never change even if product is updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    private Long productId;             // null if product was deleted
    private String productName;         // snapshot — never changes
    private String productImageUrl;     // snapshot thumbnail
    private BigDecimal priceAtPurchase; // snapshot price
    private Integer quantity;
    private BigDecimal totalPrice;      // priceAtPurchase × quantity
}
