package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Delivery charge slab — determines shipping cost based on total order weight.
 * Slabs are non-overlapping weight ranges (in grams).
 * Admin can edit these via the Shipping Config page.
 */
@Entity
@Table(name = "delivery_charge_slabs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryChargeSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_weight_grams", nullable = false)
    private Integer minWeightGrams;

    @Column(name = "max_weight_grams", nullable = false)
    private Integer maxWeightGrams;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal charge;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
