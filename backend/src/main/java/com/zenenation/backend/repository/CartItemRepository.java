package com.zenenation.backend.repository;

import com.zenenation.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find a specific product inside a specific cart.
     * Used to check if product already exists in cart before adding.
     * If exists → increment quantity. If not → create new CartItem.
     */
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /**
     * Remove a specific product from a specific cart.
     * Used when user clicks "Remove" on a cart item.
     */
    void deleteByCartIdAndProductId(Long cartId, Long productId);

    /**
     * Remove all items from a cart.
     * Called after a successful order placement to clear the cart.
     */
    void deleteByCartId(Long cartId);
}
