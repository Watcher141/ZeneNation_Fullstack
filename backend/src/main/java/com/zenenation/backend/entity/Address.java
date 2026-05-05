package com.zenenation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A saved delivery address belonging to a User.
 *
 * One user can save multiple addresses (home, office etc.)
 * One address is marked as default for faster checkout.
 *
 * Addresses are SOFT referenced by orders — when an order is placed,
 * the address fields are COPIED into the order (snapshot).
 * This means even if user deletes this address later,
 * the order still shows the correct delivery address. (see Order entity)
 */
@Entity
@Table(
    name = "addresses",
    indexes = {
        // Fast lookup of all addresses for a user
        @Index(name = "idx_addresses_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ------------------------------------------------------------------
    // OWNER
    // ------------------------------------------------------------------

    /**
     * The user this address belongs to.
     * ManyToOne — many addresses can belong to one user.
     * Lazy fetch — don't load the full user object unless needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ------------------------------------------------------------------
    // ADDRESS FIELDS
    // ------------------------------------------------------------------

    @Column(nullable = false)
    private String name;               // Recipient name (may differ from account name)

    @Column(nullable = false)
    private String phoneNumber;        // Delivery contact number

    @Column(nullable = false)
    private String addressLine1;       // House/flat no, building name

    private String addressLine2;       // Street, locality (optional)

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false)
    @Builder.Default
    private String country = "India";  // Default India, supports international later

    // ------------------------------------------------------------------
    // DEFAULT FLAG
    // ------------------------------------------------------------------

    /**
     * Whether this is the user's default delivery address.
     * Only ONE address per user should have this as true.
     * Enforced in the service layer (not DB constraint — easier to manage).
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    // ------------------------------------------------------------------
    // AUDIT FIELDS
    // ------------------------------------------------------------------

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
