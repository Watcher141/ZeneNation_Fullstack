package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.CartItemRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.CartResponse;
import com.zenenation.backend.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart", description = "Shopping cart management (login required)")
public class CartController {

    private final CartService cartService;

    /**
     * GET /api/v1/cart
     * View current cart with items and totals.
     */
    @GetMapping
    @Operation(summary = "Get current cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", cartService.getCart()));
    }

    /**
     * POST /api/v1/cart/items
     * Add a product to cart or increment quantity if already present.
     */
    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartItemRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Item added to cart",
                cartService.addToCart(request)));
    }

    /**
     * PUT /api/v1/cart/items/{cartItemId}
     * Update quantity. Send quantity=0 to remove the item.
     */
    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Update cart item quantity (0 = remove)")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Cart updated",
                cartService.updateCartItem(cartItemId, request)));
    }

    /**
     * DELETE /api/v1/cart/items/{cartItemId}
     * Remove a specific item from cart.
     */
    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long cartItemId) {

        return ResponseEntity.ok(ApiResponse.success("Item removed from cart",
                cartService.removeFromCart(cartItemId)));
    }

    /**
     * DELETE /api/v1/cart
     * Clear all items from cart.
     */
    @DeleteMapping
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart() {
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cartService.clearCart()));
    }
}
