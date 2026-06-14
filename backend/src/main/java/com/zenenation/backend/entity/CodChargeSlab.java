package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * COD charge slab — determines extra charge for Cash on Delivery orders
 * based on the order subtotal amount.
 * Slabs are non-overlapping amount ranges.
 * Admin can edit these via the Shipping Config page.
 */
@Entity
@Table(name = "cod_charge_slabs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CodChargeSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxOrderAmount;

    @Column(name = "extra_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraCharge;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
