package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.request.HomeSectionRequest;
import com.zenenation.backend.dto.response.HomeSectionResponse;
import com.zenenation.backend.dto.response.ProductSummaryResponse;
import com.zenenation.backend.entity.HomeSection;
import com.zenenation.backend.entity.HomeSectionProduct;
import com.zenenation.backend.entity.Product;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.HomeSectionProductRepository;
import com.zenenation.backend.repository.HomeSectionRepository;
import com.zenenation.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeSectionServiceImpl {

    private final HomeSectionRepository sectionRepo;
    private final HomeSectionProductRepository sectionProductRepo;
    private final ProductRepository productRepo;
    private final ProductServiceImpl productService;

    // ── Public ──

    @Transactional(readOnly = true)
    public List<HomeSectionResponse> getActiveSections() {
        return sectionRepo.findActiveWithProducts().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Admin ──

    @Transactional(readOnly = true)
    public List<HomeSectionResponse> getAllSections() {
        return sectionRepo.findAllWithProducts().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public HomeSectionResponse createSection(HomeSectionRequest req) {
        HomeSection section = HomeSection.builder()
                .title(req.getTitle())
                .subtitle(req.getSubtitle())
                .type(req.getType() != null ? req.getType() : "CUSTOM")
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .viewAllUrl(req.getViewAllUrl())
                .build();
        section = sectionRepo.save(section);
        log.info("Home section created: {}", section.getTitle());
        return toResponse(section);
    }

    @Transactional
    public HomeSectionResponse updateSection(Long id, HomeSectionRequest req) {
        HomeSection section = sectionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HomeSection", "id", id));
        section.setTitle(req.getTitle());
        if (req.getSubtitle() != null) section.setSubtitle(req.getSubtitle());
        if (req.getType() != null) section.setType(req.getType());
        if (req.getDisplayOrder() != null) section.setDisplayOrder(req.getDisplayOrder());
        if (req.getIsActive() != null) section.setIsActive(req.getIsActive());
        if (req.getViewAllUrl() != null) section.setViewAllUrl(req.getViewAllUrl());
        section = sectionRepo.save(section);
        return toResponse(section);
    }

    @Transactional
    public void deleteSection(Long id) {
        HomeSection section = sectionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HomeSection", "id", id));
        sectionRepo.delete(section);
        log.info("Home section deleted: id={}", id);
    }

    @Transactional
    public void toggleSection(Long id) {
        HomeSection section = sectionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HomeSection", "id", id));
        section.setIsActive(!section.getIsActive());
        sectionRepo.save(section);
    }

    // ── Product management ──

    @Transactional
    public HomeSectionResponse addProduct(Long sectionId, Long productId) {
        HomeSection section = sectionRepo.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("HomeSection", "id", sectionId));
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Skip if already exists
        if (sectionProductRepo.findBySectionIdAndProductId(sectionId, productId).isPresent()) {
            return toResponse(section);
        }

        int order = sectionProductRepo.countBySectionId(sectionId);
        HomeSectionProduct sp = HomeSectionProduct.builder()
                .section(section)
                .product(product)
                .displayOrder(order)
                .build();
        sectionProductRepo.save(sp);
        log.info("Product {} added to section {}", productId, sectionId);

        return toResponse(sectionRepo.findById(sectionId).get());
    }

    @Transactional
    public void removeProduct(Long sectionId, Long productId) {
        sectionProductRepo.deleteBySectionIdAndProductId(sectionId, productId);
        log.info("Product {} removed from section {}", productId, sectionId);
    }

    // ── Mapper ──

    // ── Mapper ──
    /*
        Instead of mapping products to DTOs one-by-one (which was triggering the N+1 loop trap), we updated it to gather all the raw Product entities into a list first, and then hand that entire list over to the new bulk method
           - 7/6/2026
     */
    private HomeSectionResponse toResponse(HomeSection section) {
        // 1. Gather all the raw Product entities from this section first
        List<Product> rawProducts = section.getSectionProducts() == null ? List.of() :
                section.getSectionProducts().stream()
                        .filter(sp -> sp.getProduct() != null)
                        .map(HomeSectionProduct::getProduct)
                        .collect(Collectors.toList());

        // 2. Pass the entire list to our new optimized bulk method!
        List<ProductSummaryResponse> products = productService.toSummaryResponseBulkPublic(rawProducts);

        return HomeSectionResponse.builder()
                .id(section.getId())
                .title(section.getTitle())
                .subtitle(section.getSubtitle())
                .type(section.getType())
                .displayOrder(section.getDisplayOrder())
                .isActive(section.getIsActive())
                .viewAllUrl(section.getViewAllUrl())
                .products(products)
                .productCount(products.size())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}