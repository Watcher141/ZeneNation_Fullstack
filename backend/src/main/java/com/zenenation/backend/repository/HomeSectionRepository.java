package com.zenenation.backend.repository;

import com.zenenation.backend.entity.HomeSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeSectionRepository extends JpaRepository<HomeSection, Long> {

    @Query("SELECT DISTINCT s FROM HomeSection s LEFT JOIN FETCH s.sectionProducts sp LEFT JOIN FETCH sp.product WHERE s.isActive = true ORDER BY s.displayOrder ASC")
    List<HomeSection> findActiveWithProducts();

    @Query("SELECT DISTINCT s FROM HomeSection s LEFT JOIN FETCH s.sectionProducts sp LEFT JOIN FETCH sp.product ORDER BY s.displayOrder ASC")
    List<HomeSection> findAllWithProducts();
}