package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Single image info returned in product detail.
 * imagePublicId is intentionally excluded — internal Cloudinary key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponse {

    private Long id;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
    private String altText;
}
