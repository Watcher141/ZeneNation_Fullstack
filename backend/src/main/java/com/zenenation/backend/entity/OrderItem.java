package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A single product line inside an Order.
 *
 * IMPORTANT — Price Snapshot:
 * priceAtPurchase stores the product price AT THE TIME of ordering.
 * We do NOT use product.getPrice() for order history.
 *
 * Why?
 * Product prices change over time (sales, inflation, repricing).
 * An order placed in January for ₹999 must always show ₹999,
 * even if the product is now ₹1299.
 * This is both a legal requirement and critical for correct invoice generation.
 *
 * Similarly, productName is stored as a snapshot — if a product is
 * renamed or deleted, order history still shows the original name.
 */
@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // RELATIONSHIPS
    // ------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Reference to the original product (for admin analytics).
     * Nullable because product might be soft-deleted later.
     * Even if null, productName and priceAtPurchase preserve the history.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    // ------------------------------------------------------------------
    // SNAPSHOTS (captured at order time, never change)
    // ------------------------------------------------------------------

    /**
     * Product name at time of purchase.
     * Preserved even if product is renamed or deleted.
     */
    @Column(name = "product_name", nullable = false)
    private String productName;

    /**
     * Product thumbnail URL at time of purchase.
     * Shown in order history and admin panel.
     */
    @Column(name = "product_image_url")
    private String productImageUrl;

    /**
     * Price per unit at time of purchase.
     * This is the DISCOUNTED price if a discount was active.
     * Multiply by quantity to get line total.
     */
    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    // ------------------------------------------------------------------
    // QUANTITY & TOTAL
    // ------------------------------------------------------------------

    @Column(nullable = false)
    private Integer quantity;

    /**
     * priceAtPurchase × quantity.
     * Pre-calculated and stored for fast order total display.
     * Avoids recalculating every time order is fetched.
     */
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
}
