package com.zenenation.backend.controller;

import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.ShippingConfigResponse;
import com.zenenation.backend.entity.CodChargeSlab;
import com.zenenation.backend.entity.DeliveryChargeSlab;
import com.zenenation.backend.service.ShippingConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Shipping config endpoints.
 * Public: fetch slabs for checkout display.
 * Admin: update delivery and COD charge slabs.
 */
@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingConfigController {

    private final ShippingConfigService shippingConfigService;

    // ── Public ────────────────────────────────────────────────────────────────

    /** Get all shipping config (delivery slabs + COD slabs) for checkout page */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<ShippingConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(
                "Shipping config fetched", shippingConfigService.getShippingConfig()));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** Replace all delivery charge slabs */
    @PutMapping("/admin/delivery-slabs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShippingConfigResponse>> updateDeliverySlabs(
            @RequestBody List<DeliveryChargeSlab> slabs) {
        return ResponseEntity.ok(ApiResponse.success(
                "Delivery slabs updated", shippingConfigService.updateDeliverySlabs(slabs)));
    }

    /** Replace all COD charge slabs */
    @PutMapping("/admin/cod-slabs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShippingConfigResponse>> updateCodSlabs(
            @RequestBody List<CodChargeSlab> slabs) {
        return ResponseEntity.ok(ApiResponse.success(
                "COD slabs updated", shippingConfigService.updateCodSlabs(slabs)));
    }
}
