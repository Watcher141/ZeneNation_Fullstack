package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.request.CartItemRequest;
import com.zenenation.backend.dto.response.CartItemResponse;
import com.zenenation.backend.dto.response.CartResponse;
import com.zenenation.backend.entity.Cart;
import com.zenenation.backend.entity.CartItem;
import com.zenenation.backend.entity.Product;
import com.zenenation.backend.entity.ProductImage;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.InsufficientStockException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.CartItemRepository;
import com.zenenation.backend.repository.CartRepository;
import com.zenenation.backend.repository.ProductImageRepository;
import com.zenenation.backend.repository.ProductRepository;
import com.zenenation.backend.service.CartService;
import com.zenenation.backend.util.PriceUtil;
import com.zenenation.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CART RULES:
 * - One cart per user, created at registration, persists across sessions
 * - Adding the same product twice → increments quantity (no duplicates)
 * - Quantity is validated against current stock at every add/update
 * - Cart total is ALWAYS calculated fresh from current product prices
 *   (never stored — avoids stale pricing)
 * - Max 10 units per item enforced here
 * - Cart is cleared automatically after successful order placement
 *   (called from OrderService)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final SecurityUtil securityUtil;

    private static final int MAX_QUANTITY_PER_ITEM = 10;

    // -------------------------------------------------------------------------
    // GET CART
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        Cart cart = getCurrentUserCart();
        return buildCartResponse(cart);
    }

    // -------------------------------------------------------------------------
    // ADD TO CART
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        Cart cart = getCurrentUserCart();
        Product product = getActiveProduct(request.getProductId());

        // Check if product is already in cart
        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existingItem != null) {
            // Product already in cart — increment quantity
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            validateQuantity(product, newQuantity);
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
            log.debug("Cart item quantity updated: productId={}, qty={}", product.getId(), newQuantity);
        } else {
            // New product — validate and create CartItem
            validateQuantity(product, request.getQuantity());
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
            log.debug("Product added to cart: productId={}", product.getId());
        }

        return buildCartResponse(cart);
    }

    // -------------------------------------------------------------------------
    // UPDATE CART ITEM
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, CartItemRequest request) {
        Cart cart = getCurrentUserCart();
        CartItem item = getCartItemBelongingToCart(cartItemId, cart.getId());

        // quantity = 0 means remove the item
        if (request.getQuantity() == 0) {
            cartItemRepository.delete(item);
            log.debug("Cart item removed via update with qty=0: cartItemId={}", cartItemId);
        } else {
            validateQuantity(item.getProduct(), request.getQuantity());
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
            log.debug("Cart item quantity set: cartItemId={}, qty={}", cartItemId, request.getQuantity());
        }

        return buildCartResponse(cart);
    }

    // -------------------------------------------------------------------------
    // REMOVE FROM CART
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public CartResponse removeFromCart(Long cartItemId) {
        Cart cart = getCurrentUserCart();
        CartItem item = getCartItemBelongingToCart(cartItemId, cart.getId());
        cartItemRepository.delete(item);
        log.debug("Cart item removed: cartItemId={}", cartItemId);
        return buildCartResponse(cart);
    }

    // -------------------------------------------------------------------------
    // CLEAR CART
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public CartResponse clearCart() {
        Cart cart = getCurrentUserCart();
        cartItemRepository.deleteByCartId(cart.getId());
        log.debug("Cart cleared: cartId={}", cart.getId());
        return buildCartResponse(cart);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    /**
     * Get the current authenticated user's cart.
     * Every user has exactly one cart — created at registration.
     */
    private Cart getCurrentUserCart() {
        User user = securityUtil.getCurrentUser();
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user. Please contact support."
                ));
    }

    /**
     * Get an active (not deleted, not hidden) product.
     * Prevents adding deleted/hidden products to cart.
     */
    private Product getActiveProduct(Long productId) {
        return productRepository.findByIdAndIsDeletedFalseAndIsActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    /**
     * Verify a CartItem belongs to the given cart.
     * Prevents users from modifying other users' cart items.
     */
    private CartItem getCartItemBelongingToCart(Long cartItemId, Long cartId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));

        if (!item.getCart().getId().equals(cartId)) {
            throw new BadRequestException("Cart item does not belong to your cart");
        }
        return item;
    }

    /**
     * Validate requested quantity against:
     * 1. Max quantity per item (10)
     * 2. Available stock
     */
    private void validateQuantity(Product product, int requestedQuantity) {
        if (requestedQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException(
                    "Maximum " + MAX_QUANTITY_PER_ITEM + " units allowed per item"
            );
        }
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                    product.getName(), requestedQuantity, product.getStockQuantity()
            );
        }
    }

    /**
     * Build full CartResponse with calculated totals.
     * Fetches fresh items from DB to ensure consistency after mutations.
     */
    private CartResponse buildCartResponse(Cart cart) {
        // Re-fetch items fresh from DB after any mutation
        List<CartItem> items = cartItemRepository
                .findAll()
                .stream()
                .filter(item -> item.getCart().getId().equals(cart.getId()))
                .collect(Collectors.toList());

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        // Calculate totals from current product prices
        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = itemResponses.size();
        int totalQuantity = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalQuantity(totalQuantity)
                .subtotal(PriceUtil.round(subtotal))
                .build();
    }

    /**
     * Map a CartItem entity to CartItemResponse DTO.
     * Uses CURRENT product price (not stored price) for accurate totals.
     */
    private CartItemResponse toCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        boolean isAvailable = !product.getIsDeleted()
                && product.getIsActive()
                && product.getStockQuantity() > 0;

        // Get primary image URL
        String primaryImageUrl = productImageRepository
                .findByProductIdAndIsPrimaryTrue(product.getId())
                .map(ProductImage::getImageUrl)
                .orElse(null);

        BigDecimal discountedPrice = PriceUtil.calculateDiscountedPrice(
                product.getPrice(), product.getDiscountPercent()
        );
        BigDecimal totalPrice = PriceUtil.calculateLineTotal(
                discountedPrice, item.getQuantity()
        );

        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .primaryImageUrl(primaryImageUrl)
                .unitPrice(product.getPrice())
                .discountedPrice(discountedPrice)
                .quantity(item.getQuantity())
                .totalPrice(totalPrice)
                .availableStock(product.getStockQuantity())
                .isAvailable(isAvailable)
                .isPreorder(product.getIsPreorder())
                .weightGrams(product.getWeightGrams())
                .build();
    }
}