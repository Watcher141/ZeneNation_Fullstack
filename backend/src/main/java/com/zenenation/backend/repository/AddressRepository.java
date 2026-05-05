package com.zenenation.backend.repository;

import com.zenenation.backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * Get all addresses for a user.
     * Used in "My Addresses" page and checkout address selection.
     */
    List<Address> findByUserId(Long userId);

    /**
     * Find the default address for a user.
     * Used to pre-select address at checkout.
     */
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * Count how many addresses a user has saved.
     * Used to enforce a max address limit (e.g. max 5 addresses per user).
     */
    int countByUserId(Long userId);

    /**
     * Unset default flag on ALL addresses for a user.
     * Called before setting a new default address.
     * Ensures only one address has isDefault = true at a time.
     *
     * @Modifying — required for UPDATE/DELETE queries in Spring Data JPA.
     * Without this annotation, Spring will throw an exception.
     */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void unsetDefaultForUser(@Param("userId") Long userId);
}
