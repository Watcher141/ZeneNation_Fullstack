package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.CartItemRequest;
import com.zenenation.backend.dto.response.CartResponse;

public interface CartService {

    /** Get the current user's cart with all items and calculated totals */
    CartResponse getCart();

    /**
     * Add a product to cart.
     * If product already in cart → increments quantity.
     * If new product → creates a new CartItem.
     */
    CartResponse addToCart(CartItemRequest request);

    /**
     * Update quantity of a specific item in cart.
     * If quantity = 0 → removes the item entirely.
     */
    CartResponse updateCartItem(Long cartItemId, CartItemRequest request);

    /** Remove a specific item from cart by its CartItem ID */
    CartResponse removeFromCart(Long cartItemId);

    /** Remove all items from cart */
    CartResponse clearCart();
}
