package com.zenenation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Wraps any paginated list response.
 *
 * Every listing endpoint (products, orders, users) returns this.
 * Frontend uses the pagination metadata to render page controls.
 *
 * Example response:
 * {
 *   "content": [...],
 *   "pageNumber": 0,
 *   "pageSize": 12,
 *   "totalElements": 85,
 *   "totalPages": 8,
 *   "isFirst": true,
 *   "isLast": false
 * }
 *
 * @param <T> the type of items in the list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;

    /**
     * Static factory — build directly from Spring's Page object.
     * Usage: PagedResponse.of(productPage)
     */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
