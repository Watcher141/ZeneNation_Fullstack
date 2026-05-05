package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records each time a user uses a coupon on an order.
 * Used to enforce per-user usage limits.
 */
@Entity
@Table(name = "coupon_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @CreationTimestamp
    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;
}
