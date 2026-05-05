package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A single line item inside a Cart.
 *
 * Each CartItem = one product + quantity.
 * If user adds the same product twice, quantity is incremented
 * (handled in service layer) — not two separate CartItem rows.
 *
 * Price is NOT stored here.
 * Current price is always fetched from Product at cart view time.
 * This ensures the cart always shows up-to-date prices.
 */
@Entity
@Table(
    name = "cart_items",
    indexes = {
        @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
        @Index(name = "idx_cart_items_product_id", columnList = "product_id")
    },
    // Prevent duplicate product entries in the same cart at DB level
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_cart_items_cart_product",
            columnNames = {"cart_id", "product_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // RELATIONSHIPS
    // ------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ------------------------------------------------------------------
    // QUANTITY
    // ------------------------------------------------------------------

    /**
     * How many units of this product are in the cart.
     * Minimum 1. Max enforced in service layer (e.g. max 10 per item).
     * Also validated against product stockQuantity at checkout.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    // ------------------------------------------------------------------
    // AUDIT
    // ------------------------------------------------------------------

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
