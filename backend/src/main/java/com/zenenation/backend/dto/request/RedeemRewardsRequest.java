package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RedeemRewardsRequest {

    @NotNull(message = "Points to redeem is required")
    @Min(value = 1, message = "Must redeem at least 1 point")
    private Integer pointsToRedeem;
}