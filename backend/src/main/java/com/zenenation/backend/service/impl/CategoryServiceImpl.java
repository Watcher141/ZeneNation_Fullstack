package com.zenenation.backend.service.impl;

import com.zenenation.backend.config.CacheConfig;
import com.zenenation.backend.dto.request.CategoryRequest;
import com.zenenation.backend.dto.response.CategoryResponse;
import com.zenenation.backend.entity.Category;
import com.zenenation.backend.exception.BadRequestException;
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CATEGORIES)
    public List<CategoryResponse> getAllCategories() {
        // Return only top-level categories with their subcategories nested
        return categoryRepository.findByParentIsNullAndIsDeletedFalse()
                .stream()
                .map(this::toResponseWithSubcategories)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_CATEGORY, key = "#id")
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return toResponseWithSubcategories(category);
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

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category already exists with name: " + name);
        }

        Category.CategoryBuilder builder = Category.builder()
                .name(name)
                .description(request.getDescription());

        // Set parent if parentId provided
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", request.getParentId()));
            // Prevent creating subcategory of a subcategory (max 2 levels)
            if (parent.getParent() != null) {
                throw new BadRequestException("Cannot create a subcategory of a subcategory. Max 2 levels allowed.");
            }
            builder.parent(parent);
        }

        Category category = categoryRepository.save(builder.build());
        log.info("Category created: id={}, name={}, parentId={}", category.getId(), category.getName(), request.getParentId());

        return toResponseWithSubcategories(category);
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

        categoryRepository.findByNameIgnoreCaseAndIsDeletedFalse(newName)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new DuplicateResourceException("Another category already exists with name: " + newName);
                    }
                });

        category.setName(newName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        // Update parent if provided
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", request.getParentId()));
            if (parent.getParent() != null) {
                throw new BadRequestException("Cannot nest subcategory under another subcategory.");
            }
            category.setParent(parent);
        } else {
            category.setParent(null); // promote to top-level
        }

        category = categoryRepository.save(category);
        log.info("Category updated: id={}, name={}", category.getId(), category.getName());

        return toResponseWithSubcategories(category);
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
            uploadResult = cloudinaryService.replaceImage(category.getImagePublicId(), image, categoriesFolder);
        } else {
            uploadResult = cloudinaryService.uploadImage(image, categoriesFolder);
        }

        category.setImageUrl(uploadResult.get("url"));
        category.setImagePublicId(uploadResult.get("publicId"));
        category = categoryRepository.save(category);

        log.info("Category image uploaded: categoryId={}", id);
        return toResponseWithSubcategories(category);
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
        category.setIsDeleted(true);
        // Also soft-delete all subcategories
        category.getSubcategories().forEach(sub -> sub.setIsDeleted(true));
        categoryRepository.save(category);
        log.info("Category soft deleted: id={}, name={}", id, category.getName());
    }

    // -------------------------------------------------------------------------
    // ADMIN — GET ALL
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesForAdmin() {
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(this::toResponseWithSubcategories)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Category getCategoryEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    /** Maps category with nested subcategories */
    private CategoryResponse toResponseWithSubcategories(Category category) {
        List<CategoryResponse> subcategories = category.getSubcategories() == null ? List.of() :
                category.getSubcategories().stream()
                        .filter(sub -> !sub.getIsDeleted())
                        .map(this::toResponse)
                        .collect(Collectors.toList());

        CategoryResponse response = toResponse(category);
        response.setSubcategories(subcategories);
        return response;
    }

    /** Basic mapper — no nested subcategories */
    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .isDeleted(category.getIsDeleted())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .productCount(category.getProducts() != null ? category.getProducts().size() : 0)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}