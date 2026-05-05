package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /** Find active coupon by code (case-insensitive) */
    Optional<Coupon> findByCodeIgnoreCase(String code);

    /** Check if a coupon code already exists */
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    /** Get the welcome coupon for a specific user */
    @org.springframework.data.jpa.repository.Query("""
        SELECT c FROM Coupon c
        WHERE c.assignedUser.id = :userId
        AND c.isWelcomeCoupon = true
    """)
    java.util.Optional<Coupon> findWelcomeCouponByUserId(
        @org.springframework.data.repository.query.Param("userId") Long userId);
}