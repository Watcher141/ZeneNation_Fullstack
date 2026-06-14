package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CodChargeSlabResponse {
    private Long id;
    private BigDecimal minOrderAmount;
    private BigDecimal maxOrderAmount;
    private BigDecimal extraCharge;
}
