package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.ProductRequest;
import com.zenenation.backend.dto.response.*;
import com.zenenation.backend.repository.ProductRepository;
import com.zenenation.backend.repository.ProductImageRepository;
import com.zenenation.backend.service.ProductService;
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
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Browse products (public) | Admin CRUD + image management")
public class ProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    // ── PUBLIC ────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/products
     * Paginated product listing. Supports sort and direction.
     * Query params: page, size, sortBy, sortDir
     */
    @GetMapping
    @Operation(summary = "Get all active products (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "12")   int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(ApiResponse.success("Products fetched",
                productService.getAllProducts(page, size, sortBy, sortDir)));
    }

    /**
     * GET /api/v1/products/category/{categoryId}
     * Products filtered by category. Paginated.
     */
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(ApiResponse.success("Products fetched",
                productService.getProductsByCategory(categoryId, page, size)));
    }

    /**
     * GET /api/v1/products/search?keyword=phone&categoryId=1
     * Search products by keyword — optional category filter.
     */
    @GetMapping("/search")
    @Operation(summary = "Search products by keyword")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(ApiResponse.success("Search results fetched",
                productService.searchProducts(keyword, categoryId, page, size)));
    }

    /**
     * GET /api/v1/products/{id}
     * Full product detail by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Product fetched",
                productService.getProductById(id)));
    }

    /**
     * GET /api/v1/products/slug/{slug}
     * Full product detail by slug (SEO-friendly URL).
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Product fetched",
                productService.getProductBySlug(slug)));
    }

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/products/admin/all
     * All products including hidden ones. Admin only.
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: get all products including hidden")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> getAllForAdmin(
        @RequestParam(defaultValue = "0")  int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false)    String search) {  // ← added

    return ResponseEntity.ok(ApiResponse.success("Products fetched",
            productService.getAllProductsForAdmin(page, size, search)));  // ← pass search
}

    /**
     * POST /api/v1/products
     * Create a new product. Admin only.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: create product")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully",
                        productService.createProduct(request)));
    }

    /**
     * PUT /api/v1/products/{id}
     * Update product details. Admin only.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: update product")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Product updated successfully",
                productService.updateProduct(id, request)));
    }

    /**
     * PATCH /api/v1/products/{id}/visibility
     * Toggle product visibility (show/hide). Admin only.
     */
    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: toggle product visibility")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> toggleVisibility(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.success("Product visibility updated",
                productService.toggleProductVisibility(id)));
    }

    /**
     * DELETE /api/v1/products/{id}
     * Soft delete a product. Admin only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: soft delete product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    // ── ADMIN IMAGE MANAGEMENT ─────────────────────────────────────────────────

    /**
     * POST /api/v1/products/{id}/images
     * Upload one or more images for a product.
     * Accepts multipart/form-data with field name "images".
     */
    @PostMapping("/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: upload product images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> uploadImages(
            @PathVariable Long id,
            @RequestParam("images") List<MultipartFile> images) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Images uploaded successfully",
                        productService.uploadProductImages(id, images)));
    }

    /**
     * DELETE /api/v1/products/{productId}/images/{imageId}
     * Delete a specific product image.
     */
    @DeleteMapping("/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: delete a product image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {

        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully"));
    }

    /**
     * PUT /api/v1/products/{productId}/images/{imageId}/replace
     * Replace a specific product image with a new one.
     */
    @PutMapping("/{productId}/images/{imageId}/replace")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: replace a specific product image")
    public ResponseEntity<ApiResponse<Void>> replaceImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @RequestParam("image") MultipartFile image) {
        productService.replaceProductImage(productId, imageId, image);
        return ResponseEntity.ok(ApiResponse.success("Image replaced successfully", null));
    }

    /**
     * PATCH /api/v1/products/{productId}/images/{imageId}/primary
     * Set a specific image as the primary thumbnail.
     */
    @PatchMapping("/{productId}/images/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Admin: set primary product image")
    public ResponseEntity<ApiResponse<Void>> setPrimaryImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {

        productService.setPrimaryImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Primary image updated"));
    }

    /** GET /api/v1/products/preorder — all active preorder products */
    @GetMapping("/preorder")
    public ResponseEntity<ApiResponse<java.util.List<ProductSummaryResponse>>> getPreorderProducts() {
        java.util.List<ProductSummaryResponse> products = productRepository.findAllPreorderProducts()
                .stream()
                .map(p -> {
                    String primaryImageUrl = productImageRepository
                            .findByProductIdAndIsPrimaryTrue(p.getId())
                            .map(com.zenenation.backend.entity.ProductImage::getImageUrl)
                            .orElse(null);
                    return ProductSummaryResponse.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .tagline(p.getTagline())
                            .slug(p.getSlug())
                            .price(p.getPrice())
                            .discountPercent(p.getDiscountPercent())
                            .discountedPrice(com.zenenation.backend.util.PriceUtil
                                    .calculateDiscountedPrice(p.getPrice(), p.getDiscountPercent()))
                            .stockQuantity(p.getStockQuantity())
                            .isActive(p.getIsActive())
                            .isPreorder(p.getIsPreorder())
                            .estimatedShipDate(p.getEstimatedShipDate())
                            .preorderNote(p.getPreorderNote())
                            .primaryImageUrl(primaryImageUrl)
                            .category(com.zenenation.backend.dto.response.CategoryResponse.builder()
                                    .id(p.getCategory().getId())
                                    .name(p.getCategory().getName())
                                    .build())
                            .build();
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Preorder products fetched", products));
    }

    /**
        * GET /api/v1/products/newproducts
       * Newest products first — same data as GET /api/v1/products,
        * just with sort locked to createdAt DESC so the frontend doesn't
        * need to pass sortBy/sortDir for the "New Arrivals" section.
        * Modified on 16-7-2026
        */
        @GetMapping("/newproducts")
        @Operation(summary = "Get newest products first (paginated, for New Arrivals section)")
        public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> getNewProducts(
                @RequestParam(defaultValue = "0")  int page,
                @RequestParam(defaultValue = "12") int size) {

                return ResponseEntity.ok(ApiResponse.success("New arrivals fetched",
                productService.getAllProducts(page, size, "createdAt", "desc")));
        }

}