package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.ProductRequest;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.ProductDetailResponse;
import com.zenenation.backend.dto.response.ProductImageResponse;
import com.zenenation.backend.dto.response.ProductSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    // ── Public endpoints ──────────────────────────────────────────────────────

    /** Paginated list of all active products */
    PagedResponse<ProductSummaryResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    /** Paginated products by category */
    PagedResponse<ProductSummaryResponse> getProductsByCategory(Long categoryId, int page, int size);

    /** Search products by keyword — paginated */
    PagedResponse<ProductSummaryResponse> searchProducts(String keyword, Long categoryId, int page, int size);

    /** Single product detail by ID */
    ProductDetailResponse getProductById(Long id);

    /** Single product detail by slug (SEO-friendly URL) */
    ProductDetailResponse getProductBySlug(String slug);

    // ── Admin endpoints ───────────────────────────────────────────────────────

    /** Admin: paginated list of all products including hidden ones */
    PagedResponse<ProductSummaryResponse> getAllProductsForAdmin(int page, int size);

    /** Admin: create a new product */
    ProductDetailResponse createProduct(ProductRequest request);

    /** Admin: update product details */
    ProductDetailResponse updateProduct(Long id, ProductRequest request);

    /** Admin: upload one or more images for a product */
    List<ProductImageResponse> uploadProductImages(Long productId, List<MultipartFile> images);

    /** Admin: delete a single product image by image ID */
    void deleteProductImage(Long productId, Long imageId);

    /** Admin: set which image is the primary thumbnail */
    void setPrimaryImage(Long productId, Long imageId);

    /** Admin: soft delete a product */
    void deleteProduct(Long id);

    /** Admin: toggle product visibility (isActive) */
    ProductDetailResponse toggleProductVisibility(Long id);
}
