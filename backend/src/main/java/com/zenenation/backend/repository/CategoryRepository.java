package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Get all active (non-deleted) categories.
     * Used on the public-facing website navigation/listing.
     */
    List<Category> findByIsDeletedFalse();

    /**
     * Find a single active category by ID.
     * Used when fetching category details or products under a category.
     */
    Optional<Category> findByIdAndIsDeletedFalse(Long id);

    /**
     * Find active category by name (case-insensitive).
     * Used to prevent duplicate category names on creation/update.
     * Example: "Electronics" and "electronics" should not both exist.
     */
    Optional<Category> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    /**
     * Check if a category name already exists (case-insensitive).
     * Faster than above — used purely for duplicate validation.
     */
    boolean existsByNameIgnoreCase(String name);
}
