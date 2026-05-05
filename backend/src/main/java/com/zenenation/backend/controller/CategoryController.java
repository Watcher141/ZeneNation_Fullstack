package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.CategoryRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.CategoryResponse;
import com.zenenation.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Browse categories (public) | Admin CRUD")
public class CategoryController {

    private final CategoryService categoryService;

    // ── PUBLIC ────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/categories
     * All active categories. Cached. No auth needed.
     */
    @GetMapping
    @Operation(summary = "Get all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(
                ApiResponse.success("Categories fetched", categoryService.getAllCategories())
        );
    }

    /**
     * GET /api/v1/categories/{id}
     * Single category by ID. No auth needed.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Category fetched", categoryService.getCategoryById(id))
        );
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/categories/admin/all
     * All categories including soft-deleted — admin only.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: get all categories including deleted")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllForAdmin() {
        return ResponseEntity.ok(
                ApiResponse.success("All categories fetched",
                        categoryService.getAllCategoriesForAdmin())
        );
    }

    /**
     * POST /api/v1/categories
     * Create a new category.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: create category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {

        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", response));
    }

    /**
     * PUT /api/v1/categories/{id}
     * Update category name/description.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: update category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Category updated successfully",
                        categoryService.updateCategory(id, request))
        );
    }

    /**
     * POST /api/v1/categories/{id}/image
     * Upload or replace category image.
     * Separate from update — keeps JSON payload small.
     */
    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: upload category image")
    public ResponseEntity<ApiResponse<CategoryResponse>> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        return ResponseEntity.ok(
                ApiResponse.success("Category image uploaded",
                        categoryService.uploadCategoryImage(id, image))
        );
    }

    /**
     * DELETE /api/v1/categories/{id}
     * Soft delete a category.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: soft delete category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
    }
}
