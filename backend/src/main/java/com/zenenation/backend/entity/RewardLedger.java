package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward_ledger")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardLedger {

    public enum TransactionType { CREDIT, DEBIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;                     // Nullable — can be manual credit

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer points;                  // Always positive

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;            // Snapshot of balance after this tx

    @Column(nullable = false)
    private String reason;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;         // Only for CREDIT entries

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}