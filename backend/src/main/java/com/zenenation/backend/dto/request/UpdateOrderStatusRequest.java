package com.zenenation.backend.dto.request;

import com.zenenation.backend.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Used by admin to update order status.
 * Example: move order from CONFIRMED → PROCESSING → SHIPPED → DELIVERED
 * Admin can also add an internal note visible only on the admin panel.
 */
@Data
public class UpdateOrderStatusRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    // Optional internal note added by admin (not visible to user)
    private String adminNote;
}
