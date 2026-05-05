package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A single image belonging to a Product.
 *
 * One product can have multiple images.
 * One image is marked as primary — shown in product listing cards.
 * The rest are shown in the product detail page image gallery.
 *
 * Images are stored on Cloudinary.
 * We store:
 *  - imageUrl       → the CDN URL to display the image
 *  - imagePublicId  → Cloudinary's ID to DELETE the image when needed
 *
 * Without imagePublicId, we can display images but never delete them
 * from Cloudinary, leading to wasted storage and cost.
 */
@Entity
@Table(
    name = "product_images",
    indexes = {
        // Fast lookup of all images for a product
        @Index(name = "idx_product_images_product_id", columnList = "product_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // OWNER
    // ------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ------------------------------------------------------------------
    // IMAGE INFO
    // ------------------------------------------------------------------

    /**
     * Full Cloudinary CDN URL.
     * Example: https://res.cloudinary.com/yourcloud/image/upload/v123/ecommerce/products/abc.jpg
     */
    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    /**
     * Cloudinary public ID used to delete this image.
     * Example: ecommerce/products/abc
     * REQUIRED — stored at upload time, used at delete time.
     */
    @Column(name = "image_public_id", nullable = false)
    private String imagePublicId;

    /**
     * Display order of this image in the gallery.
     * Lower number = shown first.
     * Primary image should have displayOrder = 0.
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Whether this is the primary/thumbnail image for the product.
     * Only ONE image per product should have isPrimary = true.
     * Enforced in service layer.
     * This image is shown in product listing cards and search results.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Alt text for accessibility and SEO.
     * Example: "Apple iPhone 15 Pro Max - Space Black Front View"
     * Optional but good practice.
     */
    @Column(name = "alt_text")
    private String altText;

    // ------------------------------------------------------------------
    // AUDIT
    // ------------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
