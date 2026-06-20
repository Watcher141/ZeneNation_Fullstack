package com.zenenation.backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RewardWalletResponse {
    private Integer balance;          // Current redeemable points
    private Integer lifetimeEarned;
    private Integer lifetimeUsed;
    private LocalDateTime updatedAt;

    /**
     * Monetary value of current balance in rupees.
     * Formula: balance / 2  (2 pts = ₹1)
     */
    private Integer rupeeValue;

    /**
     * Maximum points redeemable on a qualifying order (≥ ₹399).
     * Formula: floor(balance × 0.60)
     */
    private Integer maxRedeemablePoints;
}