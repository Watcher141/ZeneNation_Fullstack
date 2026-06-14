package com.zenenation.backend.repository;

import com.zenenation.backend.entity.CodChargeSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodChargeSlabRepository extends JpaRepository<CodChargeSlab, Long> {
    List<CodChargeSlab> findAllByOrderByMinOrderAmountAsc();
}
