package com.zenenation.backend.enums;

/**
 * Roles assigned to every user in the system.
 *
 * ROLE_USER  → regular customer (browse, cart, order)
 * ROLE_ADMIN → full access (dashboard, product/category/order management)
 *
 * Stored as a STRING in the DB (not ordinal number)
 * so adding new roles later never corrupts existing data.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}