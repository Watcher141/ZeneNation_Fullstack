package com.zenenation.backend.controller;

import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.RewardLedgerResponse;
import com.zenenation.backend.dto.response.RewardWalletResponse;
import com.zenenation.backend.service.RewardService;
import com.zenenation.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;
    private final SecurityUtil securityUtil;

    /** GET /api/v1/rewards/wallet */
    @GetMapping("/wallet")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RewardWalletResponse>> getMyWallet() {
        return ResponseEntity.ok(
                ApiResponse.success("Wallet fetched", rewardService.getMyWallet()));
    }

    /** GET /api/v1/rewards/history?page=0&size=10 */
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<RewardLedgerResponse>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("History fetched", rewardService.getMyHistory(page, size)));
    }

    /** GET /api/v1/rewards/max-redeemable?orderTotal=500 */
    @GetMapping("/max-redeemable")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> getMaxRedeemable(
            @RequestParam double orderTotal) {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Max redeemable points",
                rewardService.getMaxRedeemablePoints(userId, orderTotal)));
    }

    /** GET /api/v1/rewards/wallet/{userId} — admin */
    @GetMapping("/wallet/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RewardWalletResponse>> getUserWallet(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                ApiResponse.success("Wallet fetched", rewardService.getWalletByUserId(userId)));
    }
}