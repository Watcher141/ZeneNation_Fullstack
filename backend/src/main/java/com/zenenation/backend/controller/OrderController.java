package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.PlaceOrderRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.OrderResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Place and manage orders (login required)")
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/v1/orders
     * Place a new order from current cart.
     * For ONLINE payment, response includes razorpayOrderId.
     */
    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {

        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    /**
     * GET /api/v1/orders
     * Paginated order history for the current user.
     */
    @GetMapping
    @Operation(summary = "Get my order history")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success("Orders fetched",
                orderService.getMyOrders(page, size)));
    }

    /**
     * GET /api/v1/orders/{orderId}
     * Get a single order detail — only if it belongs to current user.
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(ApiResponse.success("Order fetched",
                orderService.getMyOrderById(orderId)));
    }

    /**
     * PATCH /api/v1/orders/{orderId}/cancel
     * Cancel an order — only if status is PENDING or CONFIRMED.
     * Restores stock on cancellation.
     */
    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully",
                orderService.cancelOrder(orderId)));
    }
}
