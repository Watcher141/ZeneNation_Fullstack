package com.zenenation.backend.repository;

import com.zenenation.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Get all items in a specific order.
     * Used when building order detail view.
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Get all order items for a specific product.
     * Used in admin analytics — "how many units of product X were sold?"
     */
    List<OrderItem> findByProductId(Long productId);
}
