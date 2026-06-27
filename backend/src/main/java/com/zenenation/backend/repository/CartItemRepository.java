package com.zenenation.backend.repository;

import com.zenenation.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCartIdAndProductId(Long cartId, Long productId);

    void deleteByCartId(Long cartId);

    List<CartItem> findByCartId(Long cartId);


    List<CartItem> findByCartIdAndBundleGroupId(Long cartId, String bundleGroupId);
    void deleteByCartIdAndBundleGroupId(Long cartId, String bundleGroupId);
}