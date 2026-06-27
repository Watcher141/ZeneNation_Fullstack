package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
    private BigDecimal unitPrice;
    private BigDecimal discountedPrice;
    private BigDecimal bundlePrice;
    private BigDecimal effectivePrice;
    private String bundleGroupId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Integer availableStock;
    private Boolean isAvailable;
    private Boolean isPreorder;
    private Integer weightGrams;
}