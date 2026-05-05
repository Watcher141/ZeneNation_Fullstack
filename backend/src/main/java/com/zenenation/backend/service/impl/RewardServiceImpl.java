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

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardServiceImpl implements RewardService {

    private static final double REWARD_PERCENT   = 0.20;   // 20% of order total
    private static final double MAX_REDEEM_PCT   = 0.50;   // Max 50% of order can be paid via points
    private static final int    EXPIRY_MONTHS    = 12;     // Points expire in 12 months

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
                .orElse(RewardWalletResponse.builder()
                        .balance(0).lifetimeEarned(0).lifetimeUsed(0).build());
    }

    @Override
    @Transactional(readOnly = true)
    public RewardWalletResponse getWalletByUserId(Long userId) {
        return walletRepo.findByUserId(userId)
                .map(this::toWalletResponse)
                .orElse(RewardWalletResponse.builder()
                        .balance(0).lifetimeEarned(0).lifetimeUsed(0).build());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RewardLedgerResponse> getMyHistory(int page, int size) {
        User user = securityUtil.getCurrentUser();
        var entries = ledgerRepo.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(page, size));
        return PagedResponse.of(entries.map(this::toLedgerResponse));
    }

    @Override
    @Transactional
    public void creditOrderRewards(Order order) {
        User user = order.getUser();
        RewardWallet wallet = getOrCreateWallet(user);

        int pointsToCredit = (int) Math.floor(order.getTotalAmount().doubleValue() * REWARD_PERCENT);
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
                .reason("Earned 20% reward on order " + order.getOrderNumber())
                .expiresAt(LocalDateTime.now().plusMonths(EXPIRY_MONTHS))
                .build());

        log.info("Rewarded {} points to userId={} for order={}", pointsToCredit, user.getId(), order.getOrderNumber());
    }

    @Override
    @Transactional
    public int redeemPoints(Long userId, int pointsToRedeem, Order order) {
        RewardWallet wallet = walletRepo.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("No reward wallet found"));

        if (wallet.getBalance() < pointsToRedeem) {
            throw new BadRequestException("Insufficient reward points. Available: " + wallet.getBalance());
        }

        int maxAllowed = getMaxRedeemablePoints(userId, order.getTotalAmount().doubleValue());
        if (pointsToRedeem > maxAllowed) {
            throw new BadRequestException("Cannot redeem more than " + maxAllowed + " points for this order");
        }

        wallet.setBalance(wallet.getBalance() - pointsToRedeem);
        wallet.setLifetimeUsed(wallet.getLifetimeUsed() + pointsToRedeem);
        walletRepo.save(wallet);

        ledgerRepo.save(RewardLedger.builder()
                .user(order.getUser())
                .order(order)
                .transactionType(RewardLedger.TransactionType.DEBIT)
                .points(pointsToRedeem)
                .balanceAfter(wallet.getBalance())
                .reason("Redeemed " + pointsToRedeem + " points on order " + order.getOrderNumber())
                .build());

        log.info("Redeemed {} points for userId={} on order={}", pointsToRedeem, userId, order.getOrderNumber());
        return pointsToRedeem; // 1 point = ₹1
    }

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

            ledgerRepo.save(RewardLedger.builder()
                    .user(user)
                    .order(order)
                    .transactionType(RewardLedger.TransactionType.CREDIT)
                    .points(debit.getPoints())
                    .balanceAfter(wallet.getBalance())
                    .reason("Refund of redeemed points — order " + order.getOrderNumber() + " cancelled")
                    .expiresAt(LocalDateTime.now().plusMonths(EXPIRY_MONTHS))
                    .build());

            log.info("Refunded {} points to userId={} for cancelled order={}", debit.getPoints(), user.getId(), order.getOrderNumber());
        }
    }

    @Override
    public int getMaxRedeemablePoints(Long userId, double orderTotal) {
        int balance = walletRepo.findByUserId(userId)
                .map(RewardWallet::getBalance).orElse(0);
        int maxFromOrder = (int) Math.floor(orderTotal * MAX_REDEEM_PCT);
        return Math.min(balance, maxFromOrder);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private RewardWalletResponse toWalletResponse(RewardWallet w) {
        return RewardWalletResponse.builder()
                .balance(w.getBalance())
                .lifetimeEarned(w.getLifetimeEarned())
                .lifetimeUsed(w.getLifetimeUsed())
                .updatedAt(w.getUpdatedAt())
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