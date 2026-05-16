package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByIsDeletedFalse();

    Optional<Category> findByIdAndIsDeletedFalse(Long id);

    Optional<Category> findByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCase(String name);

    /** Top-level active categories with subcategories eagerly loaded */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.subcategories s WHERE c.parent IS NULL AND c.isDeleted = false ORDER BY c.name")
    List<Category> findByParentIsNullAndIsDeletedFalse();

    /** All top-level categories (including deleted) with subcategories */
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.subcategories WHERE c.parent IS NULL ORDER BY c.name")
    List<Category> findByParentIsNull();

    /** Subcategories under a specific parent */
    List<Category> findByParentIdAndIsDeletedFalse(Long parentId);
}