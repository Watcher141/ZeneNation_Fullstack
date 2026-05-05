package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.PlaceOrderRequest;
import com.zenenation.backend.dto.request.UpdateOrderStatusRequest;
import com.zenenation.backend.dto.response.OrderResponse;
import com.zenenation.backend.dto.response.PagedResponse;

public interface OrderService {

    /**
     * Place a new order from the user's current cart.
     * Validates stock, snapshots prices, creates payment record,
     * decrements stock, clears cart.
     * For ONLINE payment → also creates Razorpay order and returns razorpayOrderId.
     */
    OrderResponse placeOrder(PlaceOrderRequest request);

    /** Get paginated order history for the current user */
    PagedResponse<OrderResponse> getMyOrders(int page, int size);

    /** Get a single order by ID — only if it belongs to the current user */
    OrderResponse getMyOrderById(Long orderId);

    /**
     * Cancel an order.
     * Only allowed if order status is PENDING or CONFIRMED.
     * Restores stock quantities for all items.
     */
    OrderResponse cancelOrder(Long orderId);

    // ── Admin ──────────────────────────────────────────────────────────────

    /** Admin: get all orders — paginated, filterable by status */
    PagedResponse<OrderResponse> getAllOrders(int page, int size, String status);

    /** Admin: get single order by ID (any user's order) */
    OrderResponse getOrderByIdForAdmin(Long orderId);

    /** Admin: update order status and optionally add an internal note */
    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);
}
