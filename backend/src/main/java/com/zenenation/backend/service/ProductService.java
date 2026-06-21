package com.zenenation.backend.service;

import com.zenenation.backend.dto.request.ProductRequest;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.ProductDetailResponse;
import com.zenenation.backend.dto.response.ProductImageResponse;
import com.zenenation.backend.dto.response.ProductSummaryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    PagedResponse<ProductSummaryResponse> getAllProducts(int page, int size, String sortBy, String sortDir);
    PagedResponse<ProductSummaryResponse> getProductsByCategory(Long categoryId, int page, int size);
    PagedResponse<ProductSummaryResponse> searchProducts(String keyword, Long categoryId, int page, int size);
    ProductDetailResponse getProductById(Long id);
    ProductDetailResponse getProductBySlug(String slug);

    //  added String search
    PagedResponse<ProductSummaryResponse> getAllProductsForAdmin(int page, int size, String search);

    ProductDetailResponse createProduct(ProductRequest request);
    ProductDetailResponse updateProduct(Long id, ProductRequest request);
    List<ProductImageResponse> uploadProductImages(Long productId, List<MultipartFile> images);
    void deleteProductImage(Long productId, Long imageId);
    void setPrimaryImage(Long productId, Long imageId);
    void replaceProductImage(Long productId, Long imageId, MultipartFile image);
    void deleteProduct(Long id);
    ProductDetailResponse toggleProductVisibility(Long id);
}