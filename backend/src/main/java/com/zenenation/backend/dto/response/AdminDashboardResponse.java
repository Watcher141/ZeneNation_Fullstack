package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Returned by the admin dashboard summary API.
 * Gives admin a quick overview of the store's health at a glance.
 * All counts and totals are calculated fresh on each request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {

    // ── Order counts by status ──
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long processingOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;

    // ── Revenue ──
    private BigDecimal totalRevenue;        // Sum of all DELIVERED order totals
    private BigDecimal todayRevenue;        // Revenue from today only
    private BigDecimal thisMonthRevenue;    // Revenue from current month

    // ── Inventory ──
    private long totalProducts;
    private long activeProducts;
    private long outOfStockProducts;        // stockQuantity = 0
    private long totalCategories;

    // ── Users ──
    private long totalUsers;
    private long newUsersToday;
}
