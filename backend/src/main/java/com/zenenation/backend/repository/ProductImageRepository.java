package com.zenenation.backend.repository;

import com.zenenation.backend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * Get all images for a product ordered by display position.
     * Used in product detail page image gallery.
     */
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    /**
     * Get the primary image of a product.
     * Used in product listing cards (shows only the main thumbnail).
     */
    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);

    /**
     * Count how many images a product has.
     * Used to enforce max image limit per product (e.g. max 8 images).
     */
    int countByProductId(Long productId);

    /**
     * Unset primary flag on ALL images for a product.
     * Called before setting a new primary image.
     * Ensures only one image has isPrimary = true per product.
     */
    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.id = :productId")
    void unsetPrimaryForProduct(@Param("productId") Long productId);

    /**
     * Delete all images for a product.
     * Called when a product is hard-deleted (cleanup).
     * Note: Cloudinary images must be deleted separately via CloudinaryService
     * before calling this — otherwise orphaned images remain on Cloudinary.
     */
    void deleteByProductId(Long productId);


    /*accepts a List of product IDs using the In keyword. changed on 7/6/2026 */

    /*
        Added the new method findByProductIdInAndIsPrimaryTrue(List<Long> productIds);.
        This allows the database to fetch all 200 images in one single trip (using a SQL IN clause) instead of forcing the app to make 200 separate network calls.
     */

    List<ProductImage> findByProductIdInAndIsPrimaryTrue(List<Long> productIds);
}
