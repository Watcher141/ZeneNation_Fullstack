package com.zenenation.backend.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class FbtSetRequest {
    private String title;
    private BigDecimal discountPercent;
    private Set<Long> productIds;
    private Boolean showEverywhere;
    private Set<Long> categoryIds; // which categories to show this bundle in (ignored if showEverywhere=true)
}