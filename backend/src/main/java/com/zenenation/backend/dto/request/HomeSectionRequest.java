package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HomeSectionRequest {
    @NotBlank
    private String title;
    private String subtitle;
    private String type = "CUSTOM";
    private Integer displayOrder = 0;
    private Boolean isActive = true;
    private String viewAllUrl;
}