package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full cart view — returned when user opens their cart.
 * Total is calculated server-side — never trust client-sent totals.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long cartId;
    private List<CartItemResponse> items;
    private Integer totalItems;         // Total number of distinct products
    private Integer totalQuantity;      // Total units across all items
    private BigDecimal subtotal;        // Sum of all item totals
}