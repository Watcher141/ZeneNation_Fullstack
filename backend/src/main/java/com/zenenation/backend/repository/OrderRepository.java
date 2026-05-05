package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Order;
import com.zenenation.backend.enums.OrderStatus;
import com.zenenation.backend.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Get all orders for a specific user — paginated.
     * Used in "My Orders" page. Most recent orders first (handled by Pageable).
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * Find a specific order by ID belonging to a user.
     * The userId check prevents users from viewing other users' orders.
     * Security check at repository level — not just service level.
     */
    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    /**
     * Find order by order number.
     * Used in order tracking and admin search.
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Admin: get all orders — paginated.
     * Used in admin dashboard orders list.
     */
    Page<Order> findAll(Pageable pageable);

    /**
     * Admin: filter orders by status — paginated.
     * Example: show all PENDING orders awaiting confirmation.
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Admin: filter orders by payment method — paginated.
     * Example: show all COD orders for delivery tracking.
     */
    Page<Order> findByPaymentMethod(PaymentMethod paymentMethod, Pageable pageable);

    /**
     * Admin: get orders placed within a date range — paginated.
     * Used for daily/weekly/monthly order reports.
     */
    @Query("""
            SELECT o FROM Order o
            WHERE o.createdAt BETWEEN :startDate AND :endDate
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Admin: search orders by user email.
     * Used when admin searches "find all orders by user@email.com".
     */
    @Query("""
            SELECT o FROM Order o
            WHERE LOWER(o.user.email) LIKE LOWER(CONCAT('%', :email, '%'))
            """)
    Page<Order> findByUserEmail(@Param("email") String email, Pageable pageable);

    /**
     * Count total orders by status.
     * Used for admin dashboard summary cards
     * (e.g. "15 orders pending", "3 orders shipped").
     */
    long countByStatus(OrderStatus status);

    /**
     * Check if a user has ever ordered a specific product.
     * Future use: only allow reviews from verified purchasers.
     */
    @Query("""
            SELECT COUNT(o) > 0 FROM Order o
            JOIN o.orderItems oi
            WHERE o.user.id = :userId
            AND oi.product.id = :productId
            AND o.status = 'DELIVERED'
            """)
    boolean hasUserPurchasedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId
    );
}
