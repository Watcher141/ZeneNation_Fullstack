package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * All listing queries:
 * 1. JOIN FETCH p.category — prevents LazyInitializationException
 * 2. AND p.isPreorder = false — preorder products are EXCLUDED from all regular
 *    listings (browse, category, search, new arrivals). They only appear via
 *    findAllPreorderProducts() which is called by the dedicated preorder endpoint.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ── PUBLIC LISTINGS (non-preorder only) ──────────────────────────────────

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE p.isDeleted = false AND p.isActive = true
            AND p.isPreorder = false
            """,
           countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.isDeleted = false AND p.isActive = true
            AND p.isPreorder = false
            """)
    Page<Product> findByIsDeletedFalseAndIsActiveTrue(Pageable pageable);

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE p.category.id = :categoryId
            AND p.isDeleted = false AND p.isActive = true
            AND p.isPreorder = false
            """,
           countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.category.id = :categoryId
            AND p.isDeleted = false AND p.isActive = true
            AND p.isPreorder = false
            """)
    Page<Product> findByCategoryIdAndIsDeletedFalseAndIsActiveTrue(
            @Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE p.id = :id AND p.isDeleted = false AND p.isActive = true
            """)
    Optional<Product> findByIdAndIsDeletedFalseAndIsActiveTrue(@Param("id") Long id);

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE p.slug = :slug AND p.isDeleted = false AND p.isActive = true
            """)
    Optional<Product> findBySlugAndIsDeletedFalseAndIsActiveTrue(@Param("slug") String slug);

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE p.isDeleted = false AND p.isActive = true
            AND p.isPreorder = false
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
           countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.isDeleted = false AND p.isActive = true
            AND p.isPreorder = false
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE p.isDeleted = false AND p.isActive = true
            AND p.isPreorder = false
            AND p.category.id = :categoryId
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
           countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.isDeleted = false AND p.isActive = true
            AND p.isPreorder = false
            AND p.category.id = :categoryId
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Product> searchByKeywordAndCategory(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    boolean existsBySlug(String slug);


    // ── PREORDER ONLY ─────────────────────────────────────────────────────────

    @Query(value = """
        SELECT p FROM Product p JOIN FETCH p.category
        WHERE p.isPreorder = true
        AND p.isDeleted = false AND p.isActive = true
        ORDER BY p.createdAt DESC
    """)
    List<Product> findAllPreorderProducts();

    // ── ADMIN LISTINGS (includes preorder, shows everything) ──────────────────

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE p.isDeleted = false
            """,
           countQuery = """
            SELECT COUNT(p) FROM Product p WHERE p.isDeleted = false
            """)
    Page<Product> findByIsDeletedFalse(Pageable pageable);

    @Query(value = """
            SELECT p FROM Product p JOIN FETCH p.category
            WHERE p.category.id = :categoryId AND p.isDeleted = false
            """,
           countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE p.category.id = :categoryId AND p.isDeleted = false
            """)
    Page<Product> findByCategoryIdAndIsDeletedFalse(
            @Param("categoryId") Long categoryId, Pageable pageable);
}