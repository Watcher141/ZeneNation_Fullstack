package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.HomeSectionRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.HomeSectionResponse;
import com.zenenation.backend.service.impl.HomeSectionServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home-sections")
@RequiredArgsConstructor
public class HomeSectionController {

    private final HomeSectionServiceImpl homeSectionService;

    // ── Public ────────────────────────────────────────────────────────────────

    /** Get all active sections with their products (for homepage) */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<HomeSectionResponse>>> getActiveSections() {
        return ResponseEntity.ok(ApiResponse.success("Sections fetched", homeSectionService.getActiveSections()));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<HomeSectionResponse>>> getAllSections() {
        return ResponseEntity.ok(ApiResponse.success("Sections fetched", homeSectionService.getAllSections()));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HomeSectionResponse>> createSection(@Valid @RequestBody HomeSectionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Section created", homeSectionService.createSection(req)));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HomeSectionResponse>> updateSection(
            @PathVariable Long id, @Valid @RequestBody HomeSectionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Section updated", homeSectionService.updateSection(id, req)));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable Long id) {
        homeSectionService.deleteSection(id);
        return ResponseEntity.ok(ApiResponse.success("Section deleted", null));
    }

    @PatchMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleSection(@PathVariable Long id) {
        homeSectionService.toggleSection(id);
        return ResponseEntity.ok(ApiResponse.success("Section toggled", null));
    }

    /** Add a product to a section */
    @PostMapping("/admin/{sectionId}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HomeSectionResponse>> addProduct(
            @PathVariable Long sectionId, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Product added", homeSectionService.addProduct(sectionId, productId)));
    }

    /** Remove a product from a section */
    @DeleteMapping("/admin/{sectionId}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeProduct(
            @PathVariable Long sectionId, @PathVariable Long productId) {
        homeSectionService.removeProduct(sectionId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed", null));
    }
}