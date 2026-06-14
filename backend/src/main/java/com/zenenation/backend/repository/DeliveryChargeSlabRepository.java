package com.zenenation.backend.repository;

import com.zenenation.backend.entity.DeliveryChargeSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryChargeSlabRepository extends JpaRepository<DeliveryChargeSlab, Long> {
    List<DeliveryChargeSlab> findAllByOrderByMinWeightGramsAsc();
}
