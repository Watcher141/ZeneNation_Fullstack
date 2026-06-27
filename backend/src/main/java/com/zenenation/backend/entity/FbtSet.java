package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "fbt_set")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FbtSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "show_everywhere")
    private Boolean showEverywhere = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "fbt_set_items",
        joinColumns = @JoinColumn(name = "fbt_set_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> products = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "fbt_set_visibility",
        joinColumns = @JoinColumn(name = "fbt_set_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> visibleInCategories = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}