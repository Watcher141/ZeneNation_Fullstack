package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PRODUCT SUMMARY — used in listing pages, search results, category pages.
 * Lightweight — only what's needed for a product card.
 * Does NOT include full description or all images.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryResponse {

    private Long id;
    private String name;
    private String tagline;
    private Boolean isPreorder;
    private java.time.LocalDate estimatedShipDate;
    private String preorderNote;
    private String slug;
    private BigDecimal price;
    private BigDecimal discountPercent;
    private BigDecimal discountedPrice;     // Calculated: price - (price * discountPercent / 100)
    private Integer stockQuantity;
    private Integer weightGrams;
    private Boolean isActive;
    private String primaryImageUrl;         // Only the main thumbnail
    private CategoryResponse category;      // Category name + id for breadcrumb
    private LocalDateTime createdAt;
}