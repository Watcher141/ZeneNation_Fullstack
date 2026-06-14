package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Combined shipping configuration response.
 * Sent to frontend so checkout page can calculate delivery + COD charges client-side.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShippingConfigResponse {
    private List<DeliveryChargeSlabResponse> deliverySlabs;
    private List<CodChargeSlabResponse> codSlabs;
}
