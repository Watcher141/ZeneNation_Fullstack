package com.zenenation.backend.service.impl;

import com.zenenation.backend.config.CacheConfig;
import com.zenenation.backend.dto.request.ProductRequest;
import com.zenenation.backend.dto.response.*;
import com.zenenation.backend.entity.Category;
import com.zenenation.backend.entity.Product;
import com.zenenation.backend.entity.ProductImage;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.CategoryRepository;
import com.zenenation.backend.repository.ProductImageRepository;
import com.zenenation.backend.repository.ProductRepository;
import com.zenenation.backend.service.CloudinaryService;
import com.zenenation.backend.service.ProductService;
import com.zenenation.backend.util.PriceUtil;
import com.zenenation.backend.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;
    private final SlugUtil slugUtil;

    // Max images per product
    private static final int MAX_IMAGES_PER_PRODUCT = 8;

    @Value("${cloudinary.folders.products}")
    private String productsFolder;

    // -------------------------------------------------------------------------
    // PUBLIC — LISTINGS
    // -------------------------------------------------------------------------

    @Override
    @Cacheable(value = CacheConfig.CACHE_PRODUCTS,
               key = "'all-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortDir")
    public PagedResponse<ProductSummaryResponse> getAllProducts(
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> products = productRepository
                .findByIsDeletedFalseAndIsActiveTrue(pageable);

        return PagedResponse.of(products.map(this::toSummaryResponse));
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_PRODUCTS,
               key = "'cat-' + #categoryId + '-' + #page + '-' + #size")
    public PagedResponse<ProductSummaryResponse> getProductsByCategory(
            Long categoryId, int page, int size) {

        // Verify category exists and is active
        Category cat = categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products;

        // If this is a top-level category (has subcategories or no parent),
        // fetch products from this category AND all subcategories
        if (cat.getParent() == null) {
            products = productRepository.findByCategoryOrSubcategoriesAndIsActiveTrue(categoryId, pageable);
        } else {
            // It's a subcategory — fetch only from this subcategory
            products = productRepository.findByCategoryIdAndIsDeletedFalseAndIsActiveTrue(categoryId, pageable);
        }

        return PagedResponse.of(products.map(this::toSummaryResponse));
    }

    @Override
    public PagedResponse<ProductSummaryResponse> searchProducts(
            String keyword, Long categoryId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products;

        if (categoryId != null) {
            products = productRepository.searchByKeywordAndCategory(keyword, categoryId, pageable);
        } else {
            products = productRepository.searchByKeyword(keyword, pageable);
        }

        return PagedResponse.of(products.map(this::toSummaryResponse));
    }

    // -------------------------------------------------------------------------
    // PUBLIC — SINGLE PRODUCT
    // -------------------------------------------------------------------------

    @Override
    @Cacheable(value = CacheConfig.CACHE_PRODUCT, key = "'id-' + #id")
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndIsDeletedFalseAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return toDetailResponse(product);
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_PRODUCT, key = "'slug-' + #slug")
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsDeletedFalseAndIsActiveTrue(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return toDetailResponse(product);
    }

    // -------------------------------------------------------------------------
    // ADMIN — LISTINGS
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ProductSummaryResponse> getAllProductsForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByIsDeletedFalse(pageable);
        return PagedResponse.of(products.map(this::toSummaryResponse));
    }

    // -------------------------------------------------------------------------
    // ADMIN — CREATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public ProductDetailResponse createProduct(ProductRequest request) {
        Category category = getCategoryById(request.getCategoryId());

        String slug = slugUtil.generateUniqueSlug(request.getName());

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .tagline(request.getTagline())
                .isPreorder(request.getIsPreorder() != null ? request.getIsPreorder() : false)
                .estimatedShipDate(request.getEstimatedShipDate())
                .preorderNote(request.getPreorderNote())
                .price(request.getPrice())
                .discountPercent(request.getDiscountPercent())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .slug(slug)
                .isActive(request.getIsActive())
                .isDeleted(false)
                .build();

        product = productRepository.save(product);
        log.info("Product created: id={}, name={}", product.getId(), product.getName());

        return toDetailResponse(product);
    }

    // -------------------------------------------------------------------------
    // ADMIN — UPDATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCT, allEntries = true)
    })
    public ProductDetailResponse updateProduct(Long id, ProductRequest request) {
        Product product = getProductEntityById(id);
        Category category = getCategoryById(request.getCategoryId());

        // Regenerate slug only if name changed
        String newSlug = slugUtil.generateUniqueSlugForUpdate(
                request.getName(), product.getSlug()
        );

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setTagline(request.getTagline());
        product.setIsPreorder(request.getIsPreorder() != null ? request.getIsPreorder() : false);
        product.setEstimatedShipDate(request.getEstimatedShipDate());
        product.setPreorderNote(request.getPreorderNote());
        product.setPrice(request.getPrice());
        product.setDiscountPercent(request.getDiscountPercent());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setSlug(newSlug);
        product.setIsActive(request.getIsActive());

        product = productRepository.save(product);
        log.info("Product updated: id={}", id);

        return toDetailResponse(product);
    }

    // -------------------------------------------------------------------------
    // ADMIN — IMAGES
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCT, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public List<ProductImageResponse> uploadProductImages(
            Long productId, List<MultipartFile> images) {

        Product product = getProductEntityById(productId);

        // Enforce max image limit
        int currentCount = productImageRepository.countByProductId(productId);
        if (currentCount + images.size() > MAX_IMAGES_PER_PRODUCT) {
            throw new BadRequestException(
                    String.format("Cannot add %d images. Product already has %d/%d images.",
                            images.size(), currentCount, MAX_IMAGES_PER_PRODUCT)
            );
        }

        List<ProductImageResponse> responses = new ArrayList<>();
        boolean isFirstImage = currentCount == 0;

        for (int i = 0; i < images.size(); i++) {
            MultipartFile imageFile = images.get(i);
            Map<String, String> uploadResult = cloudinaryService.uploadImage(
                    imageFile, productsFolder
            );

            // First image uploaded to an empty product becomes primary automatically
            boolean isPrimary = isFirstImage && i == 0;

            ProductImage productImage = ProductImage.builder()
                    .product(product)
                    .imageUrl(uploadResult.get("url"))
                    .imagePublicId(uploadResult.get("publicId"))
                    .isPrimary(isPrimary)
                    .displayOrder(currentCount + i)
                    .altText(product.getName())
                    .build();

            productImage = productImageRepository.save(productImage);
            responses.add(toImageResponse(productImage));
        }

        log.info("Uploaded {} images for product: id={}", images.size(), productId);
        return responses;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCT, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public void deleteProductImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", "id", imageId));

        // Verify the image belongs to the given product
        if (!image.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Image does not belong to product: " + productId);
        }

        // Delete from Cloudinary first
        cloudinaryService.deleteImage(image.getImagePublicId());

        // Delete from DB
        productImageRepository.delete(image);

        // If deleted image was primary — auto-assign primary to next available image
        if (image.getIsPrimary()) {
            productImageRepository.findByProductIdOrderByDisplayOrderAsc(productId)
                    .stream()
                    .findFirst()
                    .ifPresent(nextImage -> {
                        nextImage.setIsPrimary(true);
                        productImageRepository.save(nextImage);
                    });
        }

        log.info("Product image deleted: imageId={}, productId={}", imageId, productId);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PRODUCT, allEntries = true)
    public void setPrimaryImage(Long productId, Long imageId) {
        // Verify product exists
        getProductEntityById(productId);

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", "id", imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Image does not belong to product: " + productId);
        }

        // Unset primary on all images for this product
        productImageRepository.unsetPrimaryForProduct(productId);

        // Set the selected image as primary
        image.setIsPrimary(true);
        productImageRepository.save(image);

        log.info("Primary image set: imageId={}, productId={}", imageId, productId);
    }

    // -------------------------------------------------------------------------
    // ADMIN — REPLACE IMAGE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_PRODUCT, allEntries = true)
    public void replaceProductImage(Long productId, Long imageId, MultipartFile image) {
        ProductImage existing = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductImage", "id", imageId));

        if (!existing.getProduct().getId().equals(productId)) {
            throw new BadRequestException("Image does not belong to product: " + productId);
        }

        // Replace on Cloudinary — deletes old, uploads new
        Map<String, String> uploadResult = cloudinaryService.replaceImage(
                existing.getImagePublicId(), image, productsFolder
        );

        existing.setImageUrl(uploadResult.get("url"));
        existing.setImagePublicId(uploadResult.get("publicId"));
        productImageRepository.save(existing);

        log.info("Product image replaced: imageId={}, productId={}", imageId, productId);
    }

    // -------------------------------------------------------------------------
    // ADMIN — DELETE / TOGGLE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCT, allEntries = true)
    })
    public void deleteProduct(Long id) {
        Product product = getProductEntityById(id);
        product.setIsDeleted(true);
        productRepository.save(product);
        log.info("Product soft deleted: id={}", id);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_PRODUCTS, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_PRODUCT, allEntries = true)
    })
    public ProductDetailResponse toggleProductVisibility(Long id) {
        Product product = getProductEntityById(id);
        product.setIsActive(!product.getIsActive());
        product = productRepository.save(product);
        log.info("Product visibility toggled: id={}, isActive={}", id, product.getIsActive());
        return toDetailResponse(product);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }

    /** Public accessor for HomeSectionServiceImpl */
    public ProductSummaryResponse toSummaryResponsePublic(Product product) {
        return toSummaryResponse(product);
    }

    private ProductSummaryResponse toSummaryResponse(Product product) {
        // Get primary image URL
        String primaryImageUrl = productImageRepository
                .findByProductIdAndIsPrimaryTrue(product.getId())
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductSummaryResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .tagline(product.getTagline())
                .isPreorder(product.getIsPreorder())
                .estimatedShipDate(product.getEstimatedShipDate())
                .preorderNote(product.getPreorderNote())
                .slug(product.getSlug())
                .price(product.getPrice())
                .discountPercent(product.getDiscountPercent())
                .discountedPrice(PriceUtil.calculateDiscountedPrice(
                        product.getPrice(), product.getDiscountPercent()))
                .stockQuantity(product.getStockQuantity())
                .isActive(product.getIsActive())
                .primaryImageUrl(primaryImageUrl)
                .category(CategoryResponse.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .build())
                .createdAt(product.getCreatedAt())
                .build();
    }

    private ProductDetailResponse toDetailResponse(Product product) {
        List<ProductImageResponse> imageResponses = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(product.getId())
                .stream()
                .map(this::toImageResponse)
                .collect(Collectors.toList());

        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .tagline(product.getTagline())
                .isPreorder(product.getIsPreorder())
                .estimatedShipDate(product.getEstimatedShipDate())
                .preorderNote(product.getPreorderNote())
                .price(product.getPrice())
                .discountPercent(product.getDiscountPercent())
                .discountedPrice(PriceUtil.calculateDiscountedPrice(
                        product.getPrice(), product.getDiscountPercent()))
                .stockQuantity(product.getStockQuantity())
                .isActive(product.getIsActive())
                .isDeleted(product.getIsDeleted())
                .images(imageResponses)
                .category(CategoryResponse.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .imageUrl(product.getCategory().getImageUrl())
                        .build())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductImageResponse toImageResponse(ProductImage image) {
        return ProductImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .altText(image.getAltText())
                .build();
    }
}