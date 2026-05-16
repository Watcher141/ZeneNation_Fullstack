package com.zenenation.backend.repository;

import com.zenenation.backend.entity.HomeSectionProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HomeSectionProductRepository extends JpaRepository<HomeSectionProduct, Long> {
    Optional<HomeSectionProduct> findBySectionIdAndProductId(Long sectionId, Long productId);
    void deleteBySectionIdAndProductId(Long sectionId, Long productId);
    int countBySectionId(Long sectionId);
}