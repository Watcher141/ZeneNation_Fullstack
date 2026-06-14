package com.zenenation.backend.dto.response;

import com.zenenation.backend.enums.OrderStatus;
import com.zenenation.backend.enums.PaymentMethod;
import com.zenenation.backend.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full order detail — shown on order detail page and admin order view.
 * Contains everything: items, delivery address, payment info, status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNumber;

    // Order status
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    // Pricing
    private BigDecimal subtotal;
    private BigDecimal deliveryCharge;
    private BigDecimal discountAmount;
    private BigDecimal codCharge;
    private BigDecimal totalAmount;

    // Items
    private List<OrderItemResponse> orderItems;

    // Delivery address snapshot
    private String deliveryName;
    private String deliveryPhone;
    private String deliveryAddressLine1;
    private String deliveryAddressLine2;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryPincode;
    private String deliveryCountry;

    // Payment info (null for COD until delivered)
    private PaymentResponse payment;

    // User info (for admin view)
    private String userEmail;
    private String userName;

    // Notes
    private String userNote;
    private String adminNote;   // Only visible in admin response

    // Razorpay order ID — returned when ONLINE payment order is created
    // Frontend uses this to open Razorpay checkout modal
    private String razorpayOrderId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
