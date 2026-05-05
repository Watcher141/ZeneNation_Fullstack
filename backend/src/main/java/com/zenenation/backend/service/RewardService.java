package com.zenenation.backend.service;

import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.RewardLedgerResponse;
import com.zenenation.backend.dto.response.RewardWalletResponse;
import com.zenenation.backend.entity.Order;

public interface RewardService {

    /** Get current user's wallet balance */
    RewardWalletResponse getMyWallet();

    /** Get wallet for any user — admin use */
    RewardWalletResponse getWalletByUserId(Long userId);

    /** Get current user's transaction history */
    PagedResponse<RewardLedgerResponse> getMyHistory(int page, int size);

    /**
     * Credit 20% of order total as reward points.
     * Called automatically when order status → DELIVERED.
     */
    void creditOrderRewards(Order order);

    /**
     * Debit points when redeemed at checkout.
     * Returns the discount amount (= pointsToRedeem since 1pt = ₹1).
     */
    int redeemPoints(Long userId, int pointsToRedeem, Order order);

    /**
     * Refund redeemed points if order is cancelled.
     * Called automatically when order status → CANCELLED.
     */
    void refundRedeemedPoints(Order order);

    /** Validate how many points a user can redeem for a given order total */
    int getMaxRedeemablePoints(Long userId, double orderTotal);
}