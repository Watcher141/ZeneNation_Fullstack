package com.zenenation.backend.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RewardWalletResponse {
    private Integer balance;
    private Integer lifetimeEarned;
    private Integer lifetimeUsed;
    private LocalDateTime updatedAt;
}