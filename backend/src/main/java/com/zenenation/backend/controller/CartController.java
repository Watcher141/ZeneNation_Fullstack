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

    @GetMapping
    @Operation(summary = "Get current cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success("Cart fetched", cartService.getCart()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Item added to cart",
                cartService.addToCart(request)));
    }

    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Update cart item quantity (0 = remove)")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cart updated",
                cartService.updateCartItem(cartItemId, request)));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart",
                cartService.removeFromCart(cartItemId)));
    }

    @DeleteMapping("/bundle/{bundleGroupId}")
    @Operation(summary = "Remove all items belonging to a bundle group")
    public ResponseEntity<ApiResponse<CartResponse>> removeBundleGroup(
            @PathVariable String bundleGroupId) {
        return ResponseEntity.ok(ApiResponse.success("Bundle removed from cart",
                cartService.removeBundleGroup(bundleGroupId)));
    }

    @DeleteMapping
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart() {
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cartService.clearCart()));
    }
}