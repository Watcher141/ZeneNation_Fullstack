package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.CategoryRequest;
import com.zenenation.backend.dto.response.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Contract for all category operations.
 *
 * Public methods (no auth needed):
 *   - getAllCategories()
 *   - getCategoryById()
 *
 * Admin only methods:
 *   - createCategory()
 *   - updateCategory()
 *   - uploadCategoryImage()
 *   - deleteCategory()
 *
 * Access control is enforced in SecurityConfig + @PreAuthorize on controllers.
 */
public interface CategoryService {

    /** Public: get all active categories (isDeleted = false) */
    List<CategoryResponse> getAllCategories();

    /** Public: get a single active category by ID */
    CategoryResponse getCategoryById(Long id);

    /** Admin: create a new category */
    CategoryResponse createCategory(CategoryRequest request);

    /** Admin: update category name/description */
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /**
     * Admin: upload or replace category image.
     * Separate endpoint from create/update — keeps payloads small.
     */
    CategoryResponse uploadCategoryImage(Long id, MultipartFile image);

    /** Admin: soft delete a category (sets isDeleted = true) */
    void deleteCategory(Long id);

    /** Admin: get ALL categories including soft-deleted ones */
    List<CategoryResponse> getAllCategoriesForAdmin();
}
