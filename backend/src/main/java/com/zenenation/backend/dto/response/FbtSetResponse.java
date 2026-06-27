package com.zenenation.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FbtSetResponse {

    private Long id;
    private String title;
    private BigDecimal discountPercent;
    private Boolean isActive;
    private Boolean showEverywhere;
    private List<Long> visibleInCategoryIds;
    private List<String> visibleInCategoryNames;
    private List<FbtProductItem> products;

    @Data
    @Builder
    public static class FbtProductItem {
        private Long id;
        private String name;
        private String slug;
        private BigDecimal price;
        private BigDecimal discountedPrice;
        private String primaryImageUrl;
    }
}