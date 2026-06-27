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

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart() {
        Cart cart = getCurrentUserCart();
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        Cart cart = getCurrentUserCart();
        Product product = getActiveProduct(request.getProductId());

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            validateQuantity(product, newQuantity);
            existingItem.setQuantity(newQuantity);
            if (request.getBundlePrice() != null) {
                existingItem.setBundlePrice(request.getBundlePrice());
                existingItem.setBundleGroupId(request.getBundleGroupId());
            }
            cartItemRepository.save(existingItem);
        } else {
            validateQuantity(product, request.getQuantity());
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .bundlePrice(request.getBundlePrice())
                    .bundleGroupId(request.getBundleGroupId())
                    .build();
            cartItemRepository.save(newItem);
        }

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, CartItemRequest request) {
        Cart cart = getCurrentUserCart();
        CartItem item = getCartItemBelongingToCart(cartItemId, cart.getId());

        if (request.getQuantity() == 0) {
            cartItemRepository.delete(item);
        } else {
            validateQuantity(item.getProduct(), request.getQuantity());
            item.setQuantity(request.getQuantity());
            cartItemRepository.save(item);
        }

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(Long cartItemId) {
        Cart cart = getCurrentUserCart();
        CartItem item = getCartItemBelongingToCart(cartItemId, cart.getId());
        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeBundleGroup(String bundleGroupId) {
        Cart cart = getCurrentUserCart();
        cartItemRepository.deleteByCartIdAndBundleGroupId(cart.getId(), bundleGroupId);
        log.debug("Bundle group removed: bundleGroupId={}", bundleGroupId);
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clearCart() {
        Cart cart = getCurrentUserCart();
        cartItemRepository.deleteByCartId(cart.getId());
        return buildCartResponse(cart);
    }

    private Cart getCurrentUserCart() {
        User user = securityUtil.getCurrentUser();
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user. Please contact support."));
    }

    private Product getActiveProduct(Long productId) {
        return productRepository.findByIdAndIsDeletedFalseAndIsActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    private CartItem getCartItemBelongingToCart(Long cartItemId, Long cartId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));
        if (!item.getCart().getId().equals(cartId)) {
            throw new BadRequestException("Cart item does not belong to your cart");
        }
        return item;
    }

    private void validateQuantity(Product product, int requestedQuantity) {
        if (requestedQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new BadRequestException(
                    "Maximum " + MAX_QUANTITY_PER_ITEM + " units allowed per item");
        }
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                    product.getName(), requestedQuantity, product.getStockQuantity());
        }
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

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

    private CartItemResponse toCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        boolean isAvailable = !product.getIsDeleted()
                && product.getIsActive()
                && product.getStockQuantity() > 0;

        String primaryImageUrl = productImageRepository
                .findByProductIdAndIsPrimaryTrue(product.getId())
                .map(ProductImage::getImageUrl)
                .orElse(null);

        BigDecimal discountedPrice = PriceUtil.calculateDiscountedPrice(
                product.getPrice(), product.getDiscountPercent());

        BigDecimal effectivePrice = item.getBundlePrice() != null
                ? item.getBundlePrice()
                : discountedPrice;

        BigDecimal totalPrice = PriceUtil.calculateLineTotal(effectivePrice, item.getQuantity());

        return CartItemResponse.builder()
                .cartItemId(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .primaryImageUrl(primaryImageUrl)
                .unitPrice(product.getPrice())
                .discountedPrice(discountedPrice)
                .bundlePrice(item.getBundlePrice())
                .effectivePrice(effectivePrice)
                .bundleGroupId(item.getBundleGroupId())
                .quantity(item.getQuantity())
                .totalPrice(totalPrice)
                .availableStock(product.getStockQuantity())
                .isAvailable(isAvailable)
                .isPreorder(product.getIsPreorder())
                .weightGrams(product.getWeightGrams())
                .build();
    }
}