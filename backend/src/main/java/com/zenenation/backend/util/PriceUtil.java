package com.zenenation.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralizes all price calculation logic.
 *
 * WHY A DEDICATED UTIL CLASS?
 * Price calculations happen in multiple places:
 *   - CartService (calculating cart total)
 *   - OrderService (calculating order total)
 *   - ProductService (calculating discounted price for response)
 *
 * If we inline the math everywhere, any change to rounding rules
 * must be updated in every place. One util class = one place to change.
 *
 * ALL calculations use BigDecimal with HALF_UP rounding to 2 decimal places.
 * This matches standard financial rounding (how banks and invoices round).
 *
 * Example: ₹99.995 rounds to ₹100.00, not ₹99.99
 */
public class PriceUtil {

    // Standard scale for all monetary values: 2 decimal places
    private static final int MONETARY_SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // Private constructor — this is a static utility class, never instantiate it
    private PriceUtil() {}

    /**
     * Calculate the discounted price of a product.
     *
     * Formula: price - (price × discountPercent / 100)
     *
     * Examples:
     *   calculateDiscountedPrice(1000.00, 10.00) → 900.00  (10% off)
     *   calculateDiscountedPrice(999.00, 0.00)   → 999.00  (no discount)
     *   calculateDiscountedPrice(500.00, 100.00) → 0.00    (100% off — free)
     *
     * @param price           original price
     * @param discountPercent percentage discount (0 to 100)
     * @return                final price after discount, rounded to 2 decimal places
     */
    public static BigDecimal calculateDiscountedPrice(BigDecimal price, BigDecimal discountPercent) {
        if (price == null) return BigDecimal.ZERO;
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return price.setScale(MONETARY_SCALE, ROUNDING_MODE);
        }

        // discountAmount = price × (discountPercent / 100)
        BigDecimal discountAmount = price
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), MONETARY_SCALE, ROUNDING_MODE);

        BigDecimal discountedPrice = price.subtract(discountAmount);

        // Floor at zero — discount can never make price negative
        if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING_MODE);
        }

        return discountedPrice.setScale(MONETARY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate the total price for a cart or order line item.
     *
     * Formula: unitPrice × quantity
     *
     * @param unitPrice  price per single unit
     * @param quantity   number of units
     * @return           total for this line, rounded to 2 decimal places
     */
    public static BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null || quantity <= 0) return BigDecimal.ZERO;
        return unitPrice
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(MONETARY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate the grand total of an order.
     *
     * Formula: subtotal + deliveryCharge - discountAmount
     *
     * Floors at zero — total can never be negative
     * (e.g. if a huge discount somehow exceeds subtotal + delivery).
     *
     * @param subtotal        sum of all line item totals
     * @param deliveryCharge  shipping cost (0 for free delivery)
     * @param discountAmount  coupon or promo discount (0 if none)
     * @return                final order total, rounded to 2 decimal places
     */
    public static BigDecimal calculateOrderTotal(
            BigDecimal subtotal,
            BigDecimal deliveryCharge,
            BigDecimal discountAmount) {

        BigDecimal delivery = deliveryCharge != null ? deliveryCharge : BigDecimal.ZERO;
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;

        BigDecimal total = subtotal.add(delivery).subtract(discount);

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING_MODE);
        }

        return total.setScale(MONETARY_SCALE, ROUNDING_MODE);
    }

    /**
     * Round any BigDecimal to standard monetary scale (2 decimal places).
     * Used when storing calculated values to ensure consistent precision.
     *
     * @param amount  any decimal value
     * @return        same value rounded to 2 decimal places
     */
    public static BigDecimal round(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.setScale(MONETARY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate the delivery charge based on order subtotal.
     * Simple rule: free delivery above ₹500, otherwise ₹49.
     * Modify this logic freely as your delivery pricing evolves.
     *
     * @param subtotal  order subtotal before delivery
     * @return          delivery charge to apply
     */
    public static BigDecimal calculateDeliveryCharge(BigDecimal subtotal) {
        BigDecimal freeDeliveryThreshold = new BigDecimal("500.00");
        BigDecimal standardDeliveryCharge = new BigDecimal("49.00");

        if (subtotal.compareTo(freeDeliveryThreshold) >= 0) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING_MODE);
        }

        return standardDeliveryCharge.setScale(MONETARY_SCALE, ROUNDING_MODE);
    }
}
