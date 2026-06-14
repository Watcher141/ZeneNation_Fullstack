package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DeliveryChargeSlabResponse {
    private Long id;
    private Integer minWeightGrams;
    private Integer maxWeightGrams;
    private BigDecimal charge;
}
