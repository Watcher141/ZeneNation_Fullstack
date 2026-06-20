package com.zenenation.backend.service.impl;

import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.RewardLedgerResponse;
import com.zenenation.backend.dto.response.RewardWalletResponse;
import com.zenenation.backend.entity.*;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.repository.RewardLedgerRepository;
import com.zenenation.backend.repository.RewardWalletRepository;
import com.zenenation.backend.service.RewardService;
import com.zenenation.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REWARD SYSTEM BUSINESS RULES:
 *
 * EARNING:
 *   - Points earned = floor(subtotal × 0.20)
 *   - Example: ₹500 purchase → 100 pts earned  (worth ₹50)
 *   - Rewards are credited when order status → DELIVERED
 *
 * CONVERSION:
 *   - 2 reward points = ₹1 discount
 *   - 100 pts = ₹50  |  200 pts = ₹100
 *
 * REDEEMING:
 *   - Minimum order subtotal of ₹399 required to redeem
 *   - Can redeem at most 60% of available balance
 *   - Discount = floor(redeemedPoints / 2) rupees
 *   - Example: 200 pts balance → can redeem max 120 pts = ₹60 discount
 *
 * EXPIRY:
 *   - Earned points expire 12 months after credit date
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardServiceImpl implements RewardService {

    // ─── Business constants ───────────────────────────────────────────────────

    /** 20% of order subtotal → points earned */
    private static final double REWARD_EARN_PERCENT  = 0.20;

    /** Customer can redeem up to 60% of their balance per order */
    private static final double MAX_REDEEM_BALANCE_PCT = 0.60;

    /** Minimum order subtotal required to redeem any points */
    private static final BigDecimal MIN_REDEEM_ORDER_AMOUNT = BigDecimal.valueOf(399);

    /** Points expire 12 months after credit */
    private static final int EXPIRY_MONTHS = 12;

    private final RewardWalletRepository walletRepo;
    private final RewardLedgerRepository ledgerRepo;
    private final SecurityUtil securityUtil;

    // ── Get or create wallet ──────────────────────────────────────────────────

    private RewardWallet getOrCreateWallet(User user) {
        return walletRepo.findByUserId(user.getId())
                .orElseGet(() -> walletRepo.save(
                        RewardWallet.builder().user(user).balance(0)
                                .lifetimeEarned(0).lifetimeUsed(0).build()
                ));
    }

    // ── Public API ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RewardWalletResponse getMyWallet() {
        User user = securityUtil.getCurrentUser();
        return walletRepo.findByUserId(user.getId())
                .map(this::toWalletResponse)
                .orElse(emptyWalletResponse());
    }

    @Override
    @Transactional(readOnly = true)
    public RewardWalletResponse getWalletByUserId(Long userId) {
        return walletRepo.findByUserId(userId)
                .map(this::toWalletResponse)
                .orElse(emptyWalletResponse());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RewardLedgerResponse> getMyHistory(int page, int size) {
        User user = securityUtil.getCurrentUser();
        var entries = ledgerRepo.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(page, size));
        return PagedResponse.of(entries.map(this::toLedgerResponse));
    }

    // ── CREDIT — called when order is DELIVERED ───────────────────────────────

    /**
     * Credits reward points for a delivered order.
     *
     * Formula: pointsEarned = floor(subtotal × 0.20)
     * Example: ₹500 subtotal → 100 pts  (2 pts = ₹1, so worth ₹50)
     */
    @Override
    @Transactional
    public void creditOrderRewards(Order order) {
        User user = order.getUser();
        RewardWallet wallet = getOrCreateWallet(user);

        // Earn on subtotal only (item value; excludes delivery, COD charges, discounts)
        int pointsToCredit = (int) Math.floor(order.getSubtotal().doubleValue() * REWARD_EARN_PERCENT);
        if (pointsToCredit <= 0) return;

        wallet.setBalance(wallet.getBalance() + pointsToCredit);
        wallet.setLifetimeEarned(wallet.getLifetimeEarned() + pointsToCredit);
        walletRepo.save(wallet);

        ledgerRepo.save(RewardLedger.builder()
                .user(user)
                .order(order)
                .transactionType(RewardLedger.TransactionType.CREDIT)
                .points(pointsToCredit)
                .balanceAfter(wallet.getBalance())
                .reason(String.format("Earned %d pts (20%% of ₹%.0f) on order %s",
                        pointsToCredit, order.getSubtotal().doubleValue(), order.getOrderNumber()))
                .expiresAt(LocalDateTime.now().plusMonths(EXPIRY_MONTHS))
                .build());

        log.info("Rewarded {} points to userId={} for order={}",
                pointsToCredit, user.getId(), order.getOrderNumber());
    }

    // ── DEBIT — called at checkout when user redeems points ───────────────────

    /**
     * Debits reward points at checkout and returns the rupee discount.
     *
     * Rules enforced:
     * 1. Order subtotal must be ≥ ₹399
     * 2. pointsToRedeem must not exceed 60% of current balance
     * 3. User must have sufficient balance
     *
     * Returns: floor(pointsToRedeem / 2) — the discount in rupees (2 pts = ₹1)
     */
    @Override
    @Transactional
    public int redeemPoints(Long userId, int pointsToRedeem, Order order) {
        if (pointsToRedeem <= 0) return 0;

        // Guard: minimum order amount
        if (order.getSubtotal().compareTo(MIN_REDEEM_ORDER_AMOUNT) < 0) {
            throw new BadRequestException(
                    "Minimum order of ₹" + MIN_REDEEM_ORDER_AMOUNT.intValue() +
                    " is required to redeem reward points");
        }

        RewardWallet wallet = walletRepo.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No reward wallet found"));

        // Guard: sufficient balance
        if (wallet.getBalance() < pointsToRedeem) {
            throw new BadRequestException(
                    "Insufficient reward points. Available: " + wallet.getBalance());
        }

        // Guard: 60% of balance cap
        int maxAllowed = (int) Math.floor(wallet.getBalance() * MAX_REDEEM_BALANCE_PCT);
        if (pointsToRedeem > maxAllowed) {
            throw new BadRequestException(
                    "Cannot redeem more than " + maxAllowed + " points per order " +
                    "(60% of your " + wallet.getBalance() + " pt balance)");
        }

        // Debit points
        wallet.setBalance(wallet.getBalance() - pointsToRedeem);
        wallet.setLifetimeUsed(wallet.getLifetimeUsed() + pointsToRedeem);
        walletRepo.save(wallet);

        // Rupee discount = floor(pts / 2)
        int discountRupees = pointsToRedeem / 2;

        ledgerRepo.save(RewardLedger.builder()
                .user(order.getUser())
                .order(order)
                .transactionType(RewardLedger.TransactionType.DEBIT)
                .points(pointsToRedeem)
                .balanceAfter(wallet.getBalance())
                .reason(String.format("Redeemed %d pts (₹%d off) on order %s",
                        pointsToRedeem, discountRupees, order.getOrderNumber()))
                .build());

        log.info("Redeemed {} points (₹{} discount) for userId={} on order={}",
                pointsToRedeem, discountRupees, userId, order.getOrderNumber());

        return discountRupees; // rupees to discount from order total
    }

    // ── REFUND — called when order is CANCELLED ───────────────────────────────

    @Override
    @Transactional
    public void refundRedeemedPoints(Order order) {
        List<RewardLedger> debits = ledgerRepo.findByOrderIdAndTransactionType(
                order.getId(), RewardLedger.TransactionType.DEBIT);

        for (RewardLedger debit : debits) {
            User user = debit.getUser();
            RewardWallet wallet = getOrCreateWallet(user);

            wallet.setBalance(wallet.getBalance() + debit.getPoints());
            wallet.setLifetimeUsed(Math.max(0, wallet.getLifetimeUsed() - debit.getPoints()));
            walletRepo.save(wallet);

            int refundedRupees = debit.getPoints() / 2;
            ledgerRepo.save(RewardLedger.builder()
                    .user(user)
                    .order(order)
                    .transactionType(RewardLedger.TransactionType.CREDIT)
                    .points(debit.getPoints())
                    .balanceAfter(wallet.getBalance())
                    .reason(String.format("Refund: %d pts (₹%d) returned — order %s cancelled",
                            debit.getPoints(), refundedRupees, order.getOrderNumber()))
                    .expiresAt(LocalDateTime.now().plusMonths(EXPIRY_MONTHS))
                    .build());

            log.info("Refunded {} points to userId={} for cancelled order={}",
                    debit.getPoints(), user.getId(), order.getOrderNumber());
        }
    }

    // ── Validation helper ─────────────────────────────────────────────────────

    /**
     * Returns maximum points a user can redeem for a given order.
     *
     * Rules:
     * - If orderSubtotal < ₹399 → returns 0 (cannot redeem)
     * - Otherwise → floor(balance × 0.60)
     *
     * @param userId       the user's ID
     * @param orderSubtotal order subtotal in rupees (before any discounts)
     * @return max redeemable points (always ≥ 0)
     */
    @Override
    public int getMaxRedeemablePoints(Long userId, double orderSubtotal) {
        // Minimum order check
        if (orderSubtotal < MIN_REDEEM_ORDER_AMOUNT.doubleValue()) return 0;

        int balance = walletRepo.findByUserId(userId)
                .map(RewardWallet::getBalance).orElse(0);

        return (int) Math.floor(balance * MAX_REDEEM_BALANCE_PCT);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private RewardWalletResponse toWalletResponse(RewardWallet w) {
        int balance = w.getBalance();
        int maxRedeemable = (int) Math.floor(balance * MAX_REDEEM_BALANCE_PCT);
        return RewardWalletResponse.builder()
                .balance(balance)
                .lifetimeEarned(w.getLifetimeEarned())
                .lifetimeUsed(w.getLifetimeUsed())
                .updatedAt(w.getUpdatedAt())
                .rupeeValue(balance / 2)           // 2 pts = ₹1
                .maxRedeemablePoints(maxRedeemable) // 60% of balance
                .build();
    }

    private RewardWalletResponse emptyWalletResponse() {
        return RewardWalletResponse.builder()
                .balance(0).lifetimeEarned(0).lifetimeUsed(0)
                .rupeeValue(0).maxRedeemablePoints(0)
                .build();
    }

    private RewardLedgerResponse toLedgerResponse(RewardLedger l) {
        return RewardLedgerResponse.builder()
                .id(l.getId())
                .transactionType(l.getTransactionType().name())
                .points(l.getPoints())
                .balanceAfter(l.getBalanceAfter())
                .reason(l.getReason())
                .orderNumber(l.getOrder() != null ? l.getOrder().getOrderNumber() : null)
                .expiresAt(l.getExpiresAt())
                .createdAt(l.getCreatedAt())
                .build();
    }
}