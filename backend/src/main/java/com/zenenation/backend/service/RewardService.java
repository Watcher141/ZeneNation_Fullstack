package com.zenenation.backend.service;

import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.RewardLedgerResponse;
import com.zenenation.backend.dto.response.RewardWalletResponse;
import com.zenenation.backend.entity.Order;

public interface RewardService {

    /** Get current user's wallet balance and computed display values */
    RewardWalletResponse getMyWallet();

    /** Get wallet for any user — admin use */
    RewardWalletResponse getWalletByUserId(Long userId);

    /** Get current user's transaction history */
    PagedResponse<RewardLedgerResponse> getMyHistory(int page, int size);

    /**
     * Credit reward points when order status → DELIVERED.
     * Formula: floor(subtotal × 0.20) points (2 pts = ₹1)
     * Example: ₹500 order → 100 pts earned = ₹50 value
     */
    void creditOrderRewards(Order order);

    /**
     * Debit points when redeemed at checkout.
     *
     * Enforces:
     * - Minimum order subtotal of ₹399
     * - Max 60% of current balance
     *
     * Returns the rupee discount amount = floor(pointsToRedeem / 2)
     * (i.e. 120 pts redeemed → ₹60 discount)
     */
    int redeemPoints(Long userId, int pointsToRedeem, Order order);

    /**
     * Refund redeemed points if order is CANCELLED.
     * Called automatically when order status → CANCELLED.
     */
    void refundRedeemedPoints(Order order);

    /**
     * Returns maximum points a user can redeem for a given order subtotal.
     *
     * Returns 0 if orderSubtotal < ₹399 (minimum not met).
     * Otherwise returns floor(balance × 0.60).
     *
     * @param userId        the user's ID
     * @param orderSubtotal subtotal in rupees (before discounts)
     */
    int getMaxRedeemablePoints(Long userId, double orderSubtotal);
}