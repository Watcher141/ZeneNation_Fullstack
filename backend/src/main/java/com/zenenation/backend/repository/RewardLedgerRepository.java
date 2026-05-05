package com.zenenation.backend.repository;

import com.zenenation.backend.entity.RewardLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {

    Page<RewardLedger> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<RewardLedger> findByOrderIdAndTransactionType(
            Long orderId, RewardLedger.TransactionType transactionType);
}