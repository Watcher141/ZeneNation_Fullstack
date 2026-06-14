package com.zenenation.backend.service;

import com.zenenation.backend.dto.response.ShippingConfigResponse;
import com.zenenation.backend.entity.CodChargeSlab;
import com.zenenation.backend.entity.DeliveryChargeSlab;

import java.util.List;

/**
 * Service for managing shipping configuration (delivery + COD charge slabs).
 * Used by OrderService for charge calculation and by ShippingConfigController for admin CRUD.
 */
public interface ShippingConfigService {

    /** Get all delivery charge slabs ordered by weight */
    List<DeliveryChargeSlab> getDeliverySlabs();

    /** Get all COD charge slabs ordered by amount */
    List<CodChargeSlab> getCodSlabs();

    /** Get combined shipping config for frontend display */
    ShippingConfigResponse getShippingConfig();

    /** Replace all delivery slabs (admin) */
    ShippingConfigResponse updateDeliverySlabs(List<DeliveryChargeSlab> slabs);

    /** Replace all COD slabs (admin) */
    ShippingConfigResponse updateCodSlabs(List<CodChargeSlab> slabs);
}
