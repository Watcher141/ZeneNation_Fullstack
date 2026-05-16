package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A product category managed by the Admin.
 *
 * Admin can:
 *  - Create a category with name, description, image
 *  - Edit / update a category
 *  - Soft delete a category (isDeleted = true)
 *
 * WHY SOFT DELETE?
 * If we hard-delete a category that has products,
 * those products lose their category reference and break.
 * Soft delete keeps the data intact — we just hide it from the frontend.
 * Products under a deleted category are also hidden automatically.
 */
@Entity
@Table(
    name = "categories",
    indexes = {
        @Index(name = "idx_categories_name", columnList = "name"),
        // Most queries filter by isDeleted — index makes this fast
        @Index(name = "idx_categories_is_deleted", columnList = "is_deleted")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // CATEGORY INFO
    // ------------------------------------------------------------------

    /**
     * Category name must be unique (e.g. "Electronics", "Clothing").
     * Case handling is done in the service layer before saving.
     */
    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Cloudinary URL for the category banner/thumbnail image.
     * Uploaded by admin via the image upload API.
     * NULL if admin hasn't added an image yet.
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * Cloudinary public ID — needed to DELETE the image from Cloudinary
     * when admin replaces or removes the category image.
     * Without this we'd have orphaned images on Cloudinary.
     */
    @Column(name = "image_public_id")
    private String imagePublicId;

    // ------------------------------------------------------------------
    // SOFT DELETE
    // ------------------------------------------------------------------

    /**
     * false = active category (visible on website)
     * true  = deleted category (hidden from website, kept in DB)
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ------------------------------------------------------------------
    // SUBCATEGORY (self-referencing)
    // ------------------------------------------------------------------

    /**
     * Parent category — NULL for top-level categories.
     * Set for subcategories (e.g. "Naruto Figures" → parent = "Figures")
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * Child subcategories — empty for leaf categories.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> subcategories = new ArrayList<>();

    // ------------------------------------------------------------------
    // RELATIONSHIPS
    // ------------------------------------------------------------------

    /**
     * All products under this category.
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    // ------------------------------------------------------------------
    // AUDIT FIELDS
    // ------------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}