package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A shopping cart — one per user.
 *
 * Cart is created automatically when a user registers.
 * It persists across sessions (stored in DB, not browser storage).
 * Items stay in cart until user checks out or removes them.
 *
 * Cart total is NOT stored here — it's calculated dynamically
 * in the service layer to always reflect current product prices.
 */
@Entity
@Table(
    name = "carts",
    indexes = {
        @Index(name = "idx_carts_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // OWNER
    // ------------------------------------------------------------------

    /**
     * One cart per user.
     * OneToOne — a user has exactly one cart.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ------------------------------------------------------------------
    // ITEMS
    // ------------------------------------------------------------------

    /**
     * All items currently in this cart.
     * CascadeType.ALL — saving/deleting cart affects its items.
     * orphanRemoval = true — removing item from list deletes it from DB.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // ------------------------------------------------------------------
    // AUDIT
    // ------------------------------------------------------------------

    /**
     * Last time cart was modified (item added, removed, qty changed).
     * Useful for abandoned cart tracking (future feature).
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
