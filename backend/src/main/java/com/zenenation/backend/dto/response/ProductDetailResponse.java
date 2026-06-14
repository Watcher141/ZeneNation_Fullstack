package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PRODUCT DETAIL — used on the single product page.
 * Full data including all images and complete description.
 * Slightly heavier payload — only fetched when user opens a product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String tagline;
    private Boolean isPreorder;
    private java.time.LocalDate estimatedShipDate;
    private String preorderNote;
    private BigDecimal price;
    private BigDecimal discountPercent;
    private BigDecimal discountedPrice;     // Calculated field
    private Integer stockQuantity;
    private Integer weightGrams;
    private Boolean isActive;
    private Boolean isDeleted;              // Admin only
    private List<ProductImageResponse> images;
    private CategoryResponse category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}