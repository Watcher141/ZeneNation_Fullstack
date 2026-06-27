package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.FbtSetRequest;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.FbtSetResponse;
import com.zenenation.backend.entity.Category;
import com.zenenation.backend.entity.FbtSet;
import com.zenenation.backend.entity.Product;
import com.zenenation.backend.repository.CategoryRepository;
import com.zenenation.backend.repository.FbtSetRepository;
import com.zenenation.backend.repository.ProductRepository;
import com.zenenation.backend.util.PriceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fbt-sets")
@RequiredArgsConstructor
public class FbtSetController {

    private final FbtSetRepository fbtSetRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // ── PUBLIC: fetch visible bundles for a product ──
    @GetMapping("/for-product")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<FbtSetResponse>>> getForProduct(
        @RequestParam Long categoryId,
        @RequestParam(required = false) Long parentCategoryId) {

       Long parentId = parentCategoryId != null ? parentCategoryId : categoryId;

       List<FbtSetResponse> response = fbtSetRepository
            .findVisibleBundles(categoryId, parentId)
            .stream()
            .map(this::toResponse)
            .toList();

      return ResponseEntity.ok(ApiResponse.success("FBT sets fetched", response));
   }

    // ── ADMIN: fetch all bundles ──
        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        @Transactional(readOnly = true)
        public ResponseEntity<ApiResponse<List<FbtSetResponse>>> getAllAdmin() {
                List<FbtSetResponse> response = fbtSetRepository.findAllForAdmin()
                .stream()
                .map(this::toResponse)
                .toList();
                return ResponseEntity.ok(ApiResponse.success("FBT sets fetched", response));
        }

    // ── ADMIN: create bundle ──
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<FbtSetResponse>> create(@RequestBody FbtSetRequest request) {

        if (request.getProductIds() == null
                || request.getProductIds().size() < 2
                || request.getProductIds().size() > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Bundle must contain between 2 and 10 products."));
        }

        boolean showEverywhere = Boolean.TRUE.equals(request.getShowEverywhere());

        if (!showEverywhere
                && (request.getCategoryIds() == null || request.getCategoryIds().isEmpty())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Select at least one category or enable Show Everywhere."));
        }

        List<Product> products = productRepository.findAllById(request.getProductIds());

        FbtSet fbtSet = new FbtSet();
        fbtSet.setTitle(request.getTitle());
        fbtSet.setDiscountPercent(
                request.getDiscountPercent() != null
                        ? request.getDiscountPercent()
                        : BigDecimal.ZERO);
        fbtSet.setShowEverywhere(showEverywhere);
        fbtSet.setProducts(new HashSet<>(products));

        if (!showEverywhere) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            fbtSet.setVisibleInCategories(new HashSet<>(categories));
        }

        FbtSet saved = fbtSetRepository.save(fbtSet);
        return ResponseEntity.ok(ApiResponse.success("Bundle created", toResponse(saved)));
    }

    // ── ADMIN: update bundle ──
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<FbtSetResponse>> update(
            @PathVariable Long id,
            @RequestBody FbtSetRequest request) {

        FbtSet fbtSet = fbtSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bundle not found"));

        if (request.getProductIds() == null
                || request.getProductIds().size() < 2
                || request.getProductIds().size() > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Bundle must contain between 2 and 10 products."));
        }

        boolean showEverywhere = Boolean.TRUE.equals(request.getShowEverywhere());

        if (!showEverywhere
                && (request.getCategoryIds() == null || request.getCategoryIds().isEmpty())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Select at least one category or enable Show Everywhere."));
        }

        List<Product> products = productRepository.findAllById(request.getProductIds());
        fbtSet.setTitle(request.getTitle());
        fbtSet.setDiscountPercent(
                request.getDiscountPercent() != null
                        ? request.getDiscountPercent()
                        : BigDecimal.ZERO);
        fbtSet.setShowEverywhere(showEverywhere);
        fbtSet.setProducts(new HashSet<>(products));

        if (!showEverywhere) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            fbtSet.setVisibleInCategories(new HashSet<>(categories));
        } else {
            fbtSet.setVisibleInCategories(new HashSet<>());
        }

        FbtSet saved = fbtSetRepository.save(fbtSet);
        return ResponseEntity.ok(ApiResponse.success("Bundle updated", toResponse(saved)));
    }

    // ── ADMIN: toggle active ──
    @PatchMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> toggle(@PathVariable Long id) {
        FbtSet fbtSet = fbtSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bundle not found"));
        fbtSet.setIsActive(!fbtSet.getIsActive());
        fbtSetRepository.save(fbtSet);
        return ResponseEntity.ok(ApiResponse.success("Bundle toggled"));
    }

    // ── ADMIN: delete bundle ──
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        fbtSetRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Bundle deleted"));
    }

    // ── Mapper ──
    private FbtSetResponse toResponse(FbtSet fbtSet) {
        List<FbtSetResponse.FbtProductItem> items = fbtSet.getProducts()
                .stream()
                .map(p -> {
                    String imageUrl = p.getImages().stream()
                            .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                            .findFirst()
                            .map(img -> img.getImageUrl())
                            .orElse(null);

                    return FbtSetResponse.FbtProductItem.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .slug(p.getSlug())
                            .price(p.getPrice())
                            .discountedPrice(PriceUtil.calculateDiscountedPrice(
                                    p.getPrice(), p.getDiscountPercent()))
                            .primaryImageUrl(imageUrl)
                            .build();
                })
                .toList();

        List<Long> categoryIds = fbtSet.getVisibleInCategories()
                .stream().map(c -> c.getId()).toList();

        List<String> categoryNames = fbtSet.getVisibleInCategories()
                .stream().map(c -> c.getName()).toList();

        return FbtSetResponse.builder()
                .id(fbtSet.getId())
                .title(fbtSet.getTitle())
                .discountPercent(fbtSet.getDiscountPercent())
                .isActive(fbtSet.getIsActive())
                .showEverywhere(fbtSet.getShowEverywhere())
                .visibleInCategoryIds(categoryIds)
                .visibleInCategoryNames(categoryNames)
                .products(items)
                .build();
    }
}