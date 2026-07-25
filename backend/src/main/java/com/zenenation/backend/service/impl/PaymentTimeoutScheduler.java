package com.zenenation.backend.service.impl;

import com.zenenation.backend.config.CacheConfig;
import com.zenenation.backend.entity.Order;
import com.zenenation.backend.entity.OrderItem;
import com.zenenation.backend.entity.Product;
import com.zenenation.backend.enums.OrderStatus;
import com.zenenation.backend.enums.PaymentStatus;
import com.zenenation.backend.repository.OrderItemRepository;
import com.zenenation.backend.repository.OrderRepository;
import com.zenenation.backend.repository.ProductRepository;
import com.zenenation.backend.service.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * C-01 / C-02 — Payment Timeout Recovery Scheduler
 *
 * PROBLEM:
 *   When a customer places an ONLINE order, stock is decremented and reward
 *   points are debited at placeOrder() time. If the customer then abandons the
 *   Razorpay checkout modal or their browser crashes, the order stays in
 *   PENDING/PAYMENT_PENDING status indefinitely — stock is permanently held
 *   and reward points are permanently consumed even though no money was paid.
 *
 * FIX STRATEGY (additive, non-breaking):
 *   A scheduled job runs every 15 minutes. It finds any ONLINE order that has
 *   been stuck in PENDING status for longer than 30 minutes (the Razorpay
 *   payment window). For each such order it:
 *     1. Marks the order PAYMENT_FAILED  (status + payment_status)
 *     2. Restores each product's stockQuantity
 *     3. Refunds any reward points that were debited at checkout
 *     4. Evicts product caches so the restored stock is immediately visible
 *
 * WHY NOT MOVE DECREMENT TO verifyPayment()? 
 *   That would require coordinating two services and adds race-condition risk.
 *   The scheduler approach is purely additive — no existing code paths change.
 *
 * WINDOW: 30 minutes chosen to comfortably exceed Razorpay's default session
 *   timeout while still recovering stock well before the next business day.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutScheduler {

    private static final int PAYMENT_TIMEOUT_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final RewardService rewardService;
    private final CacheManager cacheManager;

    /**
     * Runs every 15 minutes, starting 5 minutes after application boot.
     * Finds ONLINE orders stuck in PENDING for > 30 minutes and recovers them.
     */
    @Scheduled(fixedDelay = 15 * 60 * 1_000, initialDelay = 5 * 60 * 1_000)
    @Transactional
    public void recoverAbandonedPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Order> staleOrders = orderRepository.findAbandonedOnlineOrders(cutoff);

        if (staleOrders.isEmpty()) {
            log.debug("PaymentTimeoutScheduler: no abandoned online orders found");
            return;
        }

        log.info("PaymentTimeoutScheduler: found {} abandoned ONLINE order(s) to recover", staleOrders.size());

        for (Order order : staleOrders) {
            try {
                recoverOrder(order);
            } catch (Exception e) {
                // Log and continue — a failure on one order should not block others
                log.error("PaymentTimeoutScheduler: failed to recover orderId={}, error={}",
                        order.getId(), e.getMessage(), e);
            }
        }

        evictProductCaches();
    }

    private void recoverOrder(Order order) {
        log.info("PaymentTimeoutScheduler: recovering orderId={}, orderNumber={}, createdAt={}",
                order.getId(), order.getOrderNumber(), order.getCreatedAt());

        // 1. Mark order as PAYMENT_FAILED
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        if (order.getPayment() != null) {
            order.getPayment().setStatus(PaymentStatus.FAILED);
            order.getPayment().setFailureReason("Payment session expired — auto-recovered by scheduler");
        }
        orderRepository.save(order);

        // 2. Restore stock for all items
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            if (item.getProduct() != null) {
                Product product = item.getProduct();
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
                log.debug("PaymentTimeoutScheduler: restored {} units of productId={} (orderId={})",
                        item.getQuantity(), product.getId(), order.getId());
            }
        }

        // 3. Refund reward points (if any were redeemed at checkout)
        //    rewardService.refundRedeemedPoints() is a no-op when no DEBIT ledger exists
        try {
            rewardService.refundRedeemedPoints(order);
        } catch (Exception e) {
            log.warn("PaymentTimeoutScheduler: reward refund failed for orderId={}: {}",
                    order.getId(), e.getMessage());
        }

        log.info("PaymentTimeoutScheduler: successfully recovered orderId={}", order.getId());
    }

    /** Evicts product listing/detail caches after bulk stock restoration. */
    private void evictProductCaches() {
        if (cacheManager.getCache(CacheConfig.CACHE_PRODUCTS) != null) {
            cacheManager.getCache(CacheConfig.CACHE_PRODUCTS).clear();
        }
        if (cacheManager.getCache(CacheConfig.CACHE_PRODUCT) != null) {
            cacheManager.getCache(CacheConfig.CACHE_PRODUCT).clear();
        }
    }
}
