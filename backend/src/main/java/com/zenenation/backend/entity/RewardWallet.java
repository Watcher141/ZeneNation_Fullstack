package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reward_wallet")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Integer balance = 0;            // Current redeemable points

    @Column(name = "lifetime_earned", nullable = false)
    @Builder.Default
    private Integer lifetimeEarned = 0;     // Total ever earned

    @Column(name = "lifetime_used", nullable = false)
    @Builder.Default
    private Integer lifetimeUsed = 0;       // Total ever redeemed

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}