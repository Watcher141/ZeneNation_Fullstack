package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "home_section_products",
       uniqueConstraints = @UniqueConstraint(columnNames = {"section_id", "product_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HomeSectionProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private HomeSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}