package com.zenenation.backend.service.impl;

import com.zenenation.backend.config.CacheConfig;
import com.zenenation.backend.dto.request.CategoryRequest;
import com.zenenation.backend.dto.response.CategoryResponse;
import com.zenenation.backend.entity.Category;
import com.zenenation.backend.exception.DuplicateResourceException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.CategoryRepository;
import com.zenenation.backend.service.CategoryService;
import com.zenenation.backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Value("${cloudinary.folders.categories}")
    private String categoriesFolder;

    // -------------------------------------------------------------------------
    // PUBLIC
    // -------------------------------------------------------------------------

    /**
     * Returns all active categories.
     * @Cacheable — result is cached after first call.
     * Cache is evicted whenever admin creates, updates, or deletes a category.
     */
    @Override
    @Cacheable(value = CacheConfig.CACHE_CATEGORIES)
    public List<CategoryResponse> getAllCategories() {
        log.debug("Fetching all active categories from DB");
        return categoryRepository.findByIsDeletedFalse()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_CATEGORY, key = "#id")
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Fetching category by id: {}", id);
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return toResponse(category);
    }

    // -------------------------------------------------------------------------
    // ADMIN — CREATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY, allEntries = true)
    })
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.getName().trim();

        // Prevent duplicate category names (case-insensitive)
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException(
                    "Category already exists with name: " + name
            );
        }

        Category category = Category.builder()
                .name(name)
                .description(request.getDescription())
                .build();

        category = categoryRepository.save(category);
        log.info("Category created: id={}, name={}", category.getId(), category.getName());

        return toResponse(category);
    }

    // -------------------------------------------------------------------------
    // ADMIN — UPDATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY, key = "#id")
    })
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = getCategoryEntityById(id);
        String newName = request.getName().trim();

        // Check for duplicate name — but exclude current category from check
        categoryRepository.findByNameIgnoreCaseAndIsDeletedFalse(newName)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DuplicateResourceException(
                                "Another category already exists with name: " + newName
                        );
                    }
                });

        category.setName(newName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        category = categoryRepository.save(category);
        log.info("Category updated: id={}, name={}", category.getId(), category.getName());

        return toResponse(category);
    }

    // -------------------------------------------------------------------------
    // ADMIN — IMAGE UPLOAD
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY, key = "#id")
    })
    public CategoryResponse uploadCategoryImage(Long id, MultipartFile image) {
        Category category = getCategoryEntityById(id);

        Map<String, String> uploadResult;

        if (category.getImagePublicId() != null) {
            // Replace existing image
            uploadResult = cloudinaryService.replaceImage(
                    category.getImagePublicId(), image, categoriesFolder
            );
        } else {
            // Upload new image
            uploadResult = cloudinaryService.uploadImage(image, categoriesFolder);
        }

        category.setImageUrl(uploadResult.get("url"));
        category.setImagePublicId(uploadResult.get("publicId"));
        category = categoryRepository.save(category);

        log.info("Category image uploaded: categoryId={}", id);
        return toResponse(category);
    }

    // -------------------------------------------------------------------------
    // ADMIN — DELETE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_CATEGORIES, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_CATEGORY, key = "#id")
    })
    public void deleteCategory(Long id) {
        Category category = getCategoryEntityById(id);

        // Soft delete — set isDeleted flag, keep data intact
        category.setIsDeleted(true);
        categoryRepository.save(category);

        log.info("Category soft deleted: id={}, name={}", id, category.getName());
    }

    // -------------------------------------------------------------------------
    // ADMIN — GET ALL (including deleted)
    // -------------------------------------------------------------------------

    @Override
    public List<CategoryResponse> getAllCategoriesForAdmin() {
        // findAll() — returns everything including soft-deleted categories
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    /**
     * Fetch the raw Category entity — used internally by update/delete/image methods.
     * Includes soft-deleted categories (admin may want to restore one — future feature).
     */
    private Category getCategoryEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    /**
     * Map Category entity → CategoryResponse DTO.
     * imagePublicId intentionally excluded — internal Cloudinary key.
     */
    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .isDeleted(category.getIsDeleted())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
