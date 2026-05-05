package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A product listed in the store, managed by Admin.
 *
 * Admin can:
 *  - Create a product under a category
 *  - Add multiple images to a product (one marked as primary)
 *  - Edit name, description, price, stock, discount
 *  - Soft delete a product (isDeleted = true)
 *
 * Price is stored as BigDecimal — NEVER use float/double for money.
 * Float arithmetic causes rounding errors (e.g. 99.99 becomes 99.989999...).
 * BigDecimal is exact.
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_category_id", columnList = "category_id"),
        @Index(name = "idx_products_is_deleted", columnList = "is_deleted"),
        // Full text search on name (used in product search API)
        @Index(name = "idx_products_name", columnList = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // PRODUCT INFO
    // ------------------------------------------------------------------

    @Column(nullable = false)
    private String name;

    /**
     * Full product description — supports long text.
     * TEXT type in PostgreSQL (no length limit).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Short secondary text shown below the product name on the detail page.
     * Examples: "Believe it! — Limited Edition", "Hand-crafted 104cm Wooden Katana"
     * Optional — nullable.
     */
    @Column(length = 200)
    private String tagline;

    /**
     * When true:
     * - Product appears in the Preorder section on homepage
     * - Product is HIDDEN from regular category browsing
     * - When set back to false → moves to regular category, disappears from preorder
     */
    @Column(name = "is_preorder", nullable = false)
    @Builder.Default
    private Boolean isPreorder = false;

    /** Estimated shipping date shown to users on preorder products */
    @Column(name = "estimated_ship_date")
    private java.time.LocalDate estimatedShipDate;

    /** Optional note shown on preorder e.g. "Ships Q2 2025 — Limited to 500 units" */
    @Column(name = "preorder_note", length = 300)
    private String preorderNote;

    /**
     * Original price. Always stored — even if discount is applied.
     * Use BigDecimal for exact decimal arithmetic (never float/double for money).
     * precision=10 scale=2 → up to 99,999,999.99
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Discount percentage (0 to 100).
     * discountedPrice is calculated in service layer:
     * discountedPrice = price - (price * discountPercent / 100)
     * We don't store discountedPrice in DB to avoid data inconsistency.
     */
    @Column(name = "discount_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    /**
     * Available stock quantity.
     * Decremented when order is placed.
     * Incremented when order is cancelled.
     * Admin can manually update this.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    /**
     * Short string used in product URLs.
     * Example: "apple-iphone-15-pro-max"
     * Generated from name in service layer.
     * Unique across all products.
     */
    @Column(unique = true)
    private String slug;

    // ------------------------------------------------------------------
    // CATEGORY
    // ------------------------------------------------------------------

    /**
     * The category this product belongs to.
     * ManyToOne — many products can belong to one category.
     * Lazy fetch — don't load full category object unless needed.
     * nullable = false — every product must have a category.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // ------------------------------------------------------------------
    // IMAGES
    // ------------------------------------------------------------------

    /**
     * Product images (multiple allowed).
     * One image is marked as primary (shown in product listing).
     * Rest shown in product detail gallery.
     * CascadeType.ALL — deleting product deletes all its images.
     * orphanRemoval = true — removing image from list deletes it from DB.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    // ------------------------------------------------------------------
    // SOFT DELETE & VISIBILITY
    // ------------------------------------------------------------------

    /**
     * false = active product (visible on website)
     * true  = deleted product (hidden from website, kept in DB)
     * Kept in DB to preserve order history references.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Admin can temporarily hide a product without deleting it.
     * Example: out of season, being updated, temporarily unavailable.
     * false = hidden, true = visible
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ------------------------------------------------------------------
    // AUDIT FIELDS
    // ------------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}