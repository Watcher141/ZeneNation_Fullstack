package com.zenenation.backend.repository;

import com.zenenation.backend.entity.FbtSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FbtSetRepository extends JpaRepository<FbtSet, Long> {

    @Query("""
    SELECT DISTINCT f FROM FbtSet f
    JOIN FETCH f.products p
    LEFT JOIN FETCH p.images i
    LEFT JOIN FETCH f.visibleInCategories vc
    WHERE f.isActive = true
    AND (
        f.showEverywhere = true
        OR vc.id = :categoryId
        OR vc.id = :parentCategoryId
    )
    """)
    List<FbtSet> findVisibleBundles(
    @Param("categoryId") Long categoryId,
    @Param("parentCategoryId") Long parentCategoryId
    );

    @Query("""
    SELECT DISTINCT f FROM FbtSet f
    LEFT JOIN FETCH f.products p
    LEFT JOIN FETCH p.images i
    LEFT JOIN FETCH f.visibleInCategories vc
    """)
    List<FbtSet> findAllForAdmin();


    
}