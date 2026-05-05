package com.zenenation.backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RewardLedgerResponse {
    private Long id;
    private String transactionType;
    private Integer points;
    private Integer balanceAfter;
    private String reason;
    private String orderNumber;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}