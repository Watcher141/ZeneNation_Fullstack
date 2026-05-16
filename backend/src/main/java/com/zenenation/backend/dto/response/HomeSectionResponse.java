package com.zenenation.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomeSectionResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String type;
    private Integer displayOrder;
    private Boolean isActive;
    private String viewAllUrl;
    private List<ProductSummaryResponse> products;
    private Integer productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}