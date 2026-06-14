package com.zenenation.backend.entity;

import com.zenenation.backend.enums.OrderStatus;
import com.zenenation.backend.enums.PaymentMethod;
import com.zenenation.backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A placed order by a User.
 *
 * IMPORTANT DESIGN DECISION — Address Snapshot:
 * We do NOT store a foreign key to the Address entity.
 * Instead, we copy the address fields directly into the order.
 *
 * Why?
 * If a user edits or deletes their address after placing an order,
 * the order must still show the ORIGINAL delivery address.
 * This is a legal and business requirement for order records.
 *
 * Same logic applies to price — OrderItem stores price at time of purchase.
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_created_at", columnList = "created_at"),
        // Admin searches orders by payment method
        @Index(name = "idx_orders_payment_method", columnList = "payment_method")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable order number shown to user and admin.
     * Format: ORD-2024-00001 (generated in service layer).
     * Unique across all orders.
     */
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    // ------------------------------------------------------------------
    // OWNER
    // ------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ------------------------------------------------------------------
    // ORDER ITEMS
    // ------------------------------------------------------------------

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // ------------------------------------------------------------------
    // PRICING
    // ------------------------------------------------------------------

    /**
     * Sum of (item price × quantity) for all items.
     * Stored at order creation time — never changes after.
     */
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * Delivery charge applied to this order.
     * Can be 0 for free delivery promotions.
     */
    @Column(name = "delivery_charge", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryCharge = BigDecimal.ZERO;

    /**
     * Total discount applied (coupon / promo — future feature slot).
     */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Extra charge applied when payment method is COD.
     * Calculated from cod_charge_slabs based on order subtotal.
     */
    @Column(name = "cod_charge", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal codCharge = BigDecimal.ZERO;

    /**
     * Final amount paid / to be paid.
     * totalAmount = subtotal + deliveryCharge + codCharge - discountAmount
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // ------------------------------------------------------------------
    // STATUS
    // ------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // ------------------------------------------------------------------
    // DELIVERY ADDRESS SNAPSHOT
    // ------------------------------------------------------------------
    // These fields are copied from the user's selected Address at checkout.
    // They never change even if user later edits/deletes their address.

    @Column(name = "delivery_name", nullable = false)
    private String deliveryName;

    @Column(name = "delivery_phone", nullable = false)
    private String deliveryPhone;

    @Column(name = "delivery_address_line1", nullable = false)
    private String deliveryAddressLine1;

    @Column(name = "delivery_address_line2")
    private String deliveryAddressLine2;

    @Column(name = "delivery_city", nullable = false)
    private String deliveryCity;

    @Column(name = "delivery_state", nullable = false)
    private String deliveryState;

    @Column(name = "delivery_pincode", nullable = false)
    private String deliveryPincode;

    @Column(name = "delivery_country", nullable = false)
    @Builder.Default
    private String deliveryCountry = "India";

    // ------------------------------------------------------------------
    // PAYMENT RELATIONSHIP
    // ------------------------------------------------------------------

    /**
     * Payment details for this order.
     * NULL for COD orders until admin marks as delivered+paid.
     * Populated by Razorpay payment flow for online orders.
     */
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    // ------------------------------------------------------------------
    // NOTES
    // ------------------------------------------------------------------

    /**
     * Optional note from user at checkout.
     * Example: "Leave at door", "Call before delivery"
     */
    @Column(name = "user_note", columnDefinition = "TEXT")
    private String userNote;

    /**
     * Internal note added by admin.
     * Not visible to user.
     */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    // ── Preorder fields ──────────────────────────────────────────────────────

    /** True if this order contains preorder items */
    @Column(name = "is_preorder_order", nullable = false)
    @Builder.Default
    private Boolean isPreorderOrder = false;

    /**
     * HALF → user paid 50% now, owes 50% on shipping
     * FULL → user paid 100% upfront
     * NULL → regular (non-preorder) order
     */
    @Column(name = "preorder_payment_type", length = 10)
    private String preorderPaymentType;

    /** Amount still owed by user — set when preorderPaymentType = HALF */
    @Column(name = "remaining_amount", precision = 10, scale = 2)
    private java.math.BigDecimal remainingAmount;

    // ------------------------------------------------------------------
    // AUDIT
    // ------------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}