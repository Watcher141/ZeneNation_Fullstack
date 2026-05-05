package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.UpdateOrderStatusRequest;
import com.zenenation.backend.dto.response.*;
import com.zenenation.backend.service.AdminService;
import com.zenenation.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin dashboard, order and user management")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;

    // ── DASHBOARD ─────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/dashboard
     * Summary stats: orders by status, revenue, inventory, users.
     * Cached for 10 minutes.
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard summary")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard loaded",
                adminService.getDashboardSummary()));
    }

    // ── ORDER MANAGEMENT ──────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/orders
     * All orders — paginated. Filter by status using ?status=PENDING
     *
     * Shows: user email, name, delivery address, phone,
     *        products ordered, quantities, total, payment method/status.
     */
    @GetMapping("/orders")
    @Operation(summary = "Get all orders (filterable by status)")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(required = false)     String status) {

        return ResponseEntity.ok(ApiResponse.success("Orders fetched",
                orderService.getAllOrders(page, size, status)));
    }

    /**
     * GET /api/v1/admin/orders/{orderId}
     * Full detail of any order — including user info, address, all items.
     */
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get full order detail")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(ApiResponse.success("Order fetched",
                orderService.getOrderByIdForAdmin(orderId)));
    }

    /**
     * PATCH /api/v1/admin/orders/{orderId}/status
     * Update order status — move through the lifecycle.
     * Automatically marks COD payment as PAID when set to DELIVERED.
     *
     * Valid transitions:
     * PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
     * Any (before SHIPPED) → CANCELLED
     */
    @PatchMapping("/orders/{orderId}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Order status updated",
                orderService.updateOrderStatus(orderId, request)));
    }

    // ── USER MANAGEMENT ───────────────────────────────────────────────────────

    /**
     * GET /api/v1/admin/users
     * All registered users — paginated.
     * Shows: name, email, phone, role, provider, active status, joined date.
     */
    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success("Users fetched",
                adminService.getAllUsers(page, size)));
    }

    /**
     * GET /api/v1/admin/users/{userId}
     * Single user detail by ID.
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(ApiResponse.success("User fetched",
                adminService.getUserById(userId)));
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/deactivate
     * Deactivate a user account — they cannot log in until reactivated.
     * Admin cannot deactivate their own account.
     */
    @PatchMapping("/users/{userId}/deactivate")
    @Operation(summary = "Deactivate user account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> deactivateUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(ApiResponse.success("User deactivated",
                adminService.deactivateUser(userId)));
    }

    /**
     * PATCH /api/v1/admin/users/{userId}/activate
     * Reactivate a previously deactivated user.
     */
    @PatchMapping("/users/{userId}/activate")
    @Operation(summary = "Activate user account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> activateUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(ApiResponse.success("User activated",
                adminService.activateUser(userId)));
    }
}
