package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Find the cart belonging to a user.
     * Every user has exactly one cart (created on registration).
     * Returns Optional — though in practice it should always exist.
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * Find carts last updated between oldBound and newBound (abandoned window).
     * Only carts that have items — join with cart_items.
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT DISTINCT c FROM Cart c
        JOIN c.items ci
        WHERE c.updatedAt BETWEEN :oldBound AND :newBound
    """)
    java.util.List<Cart> findAbandonedCarts(
        @org.springframework.data.repository.query.Param("oldBound") java.time.LocalDateTime oldBound,
        @org.springframework.data.repository.query.Param("newBound") java.time.LocalDateTime newBound);

}