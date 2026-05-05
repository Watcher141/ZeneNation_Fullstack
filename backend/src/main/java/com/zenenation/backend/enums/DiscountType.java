package com.zenenation.backend.enums;

/**
 * Type of discount a coupon applies.
 *
 * PERCENTAGE → discount_value is a percentage (e.g. 20 = 20% off)
 *              Can have a maximum_discount cap
 *              Example: 20% off, max ₹500 discount
 *
 * FLAT       → discount_value is a fixed rupee amount (e.g. 100 = ₹100 off)
 *              Example: flat ₹100 off on orders above ₹500
 */
public enum DiscountType {
    PERCENTAGE,
    FLAT
}
