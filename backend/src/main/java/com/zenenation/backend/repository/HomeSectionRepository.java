package com.zenenation.backend.repository;

import com.zenenation.backend.entity.HomeSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeSectionRepository extends JpaRepository<HomeSection, Long> {

    /**
     * Fetch active sections with products AND their categories eagerly loaded.
     * Without JOIN FETCH on p.category, accessing product.getCategory() in the mapper
     * throws LazyInitializationException because the Hibernate session has already closed.
     */
    @Query("SELECT DISTINCT s FROM HomeSection s LEFT JOIN FETCH s.sectionProducts sp LEFT JOIN FETCH sp.product p LEFT JOIN FETCH p.category WHERE s.isActive = true ORDER BY s.displayOrder ASC")
    List<HomeSection> findActiveWithProducts();

    @Query("SELECT DISTINCT s FROM HomeSection s LEFT JOIN FETCH s.sectionProducts sp LEFT JOIN FETCH sp.product p LEFT JOIN FETCH p.category ORDER BY s.displayOrder ASC")
    List<HomeSection> findAllWithProducts();
}