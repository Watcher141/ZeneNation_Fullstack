package com.zenenation.backend.util;

import com.zenenation.backend.entity.CodChargeSlab;
import com.zenenation.backend.entity.DeliveryChargeSlab;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
     * Formula: subtotal + deliveryCharge + codCharge - discountAmount
     *
     * Floors at zero — total can never be negative
     * (e.g. if a huge discount somehow exceeds subtotal + delivery).
     *
     * @param subtotal        sum of all line item totals
     * @param deliveryCharge  shipping cost based on weight
     * @param codCharge       extra charge for COD orders (0 for online)
     * @param discountAmount  coupon or promo discount (0 if none)
     * @return                final order total, rounded to 2 decimal places
     */
    public static BigDecimal calculateOrderTotal(
            BigDecimal subtotal,
            BigDecimal deliveryCharge,
            BigDecimal codCharge,
            BigDecimal discountAmount) {

        BigDecimal delivery = deliveryCharge != null ? deliveryCharge : BigDecimal.ZERO;
        BigDecimal cod = codCharge != null ? codCharge : BigDecimal.ZERO;
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;

        BigDecimal total = subtotal.add(delivery).add(cod).subtract(discount);

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
     * Calculate delivery charge based on total order weight using slab pricing.
     *
     * Looks up the matching slab for the given weight.
     * If weight exceeds all slabs, uses the highest slab's charge.
     * If weight is 0 or slabs are empty, returns the first slab's charge as minimum.
     *
     * @param totalWeightGrams  total weight of all items in grams
     * @param slabs             ordered list of delivery charge slabs (asc by minWeight)
     * @return                  delivery charge for this weight
     */
    public static BigDecimal calculateDeliveryCharge(int totalWeightGrams, List<DeliveryChargeSlab> slabs) {
        if (slabs == null || slabs.isEmpty()) {
            // Fallback: no slabs configured — free delivery
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING_MODE);
        }

        // If weight is 0 or less, use the first (lightest) slab
        int weight = Math.max(totalWeightGrams, 1);

        for (DeliveryChargeSlab slab : slabs) {
            if (weight >= slab.getMinWeightGrams() && weight <= slab.getMaxWeightGrams()) {
                return slab.getCharge().setScale(MONETARY_SCALE, ROUNDING_MODE);
            }
        }

        // Weight exceeds all slabs — use the highest slab's charge
        DeliveryChargeSlab highest = slabs.get(slabs.size() - 1);
        return highest.getCharge().setScale(MONETARY_SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate COD extra charge based on order subtotal using slab pricing.
     *
     * Only applied when payment method is COD.
     * Looks up the matching slab for the given subtotal.
     * If subtotal exceeds all slabs, uses the highest slab's charge.
     *
     * @param subtotal  order subtotal (sum of item prices)
     * @param slabs     ordered list of COD charge slabs (asc by minOrderAmount)
     * @return          extra COD charge for this order amount
     */
    public static BigDecimal calculateCodCharge(BigDecimal subtotal, List<CodChargeSlab> slabs) {
        if (slabs == null || slabs.isEmpty() || subtotal == null) {
            return BigDecimal.ZERO.setScale(MONETARY_SCALE, ROUNDING_MODE);
        }

        for (CodChargeSlab slab : slabs) {
            if (subtotal.compareTo(slab.getMinOrderAmount()) >= 0
                    && subtotal.compareTo(slab.getMaxOrderAmount()) <= 0) {
                return slab.getExtraCharge().setScale(MONETARY_SCALE, ROUNDING_MODE);
            }
        }

        // Subtotal exceeds all slabs — use the highest slab's charge
        CodChargeSlab highest = slabs.get(slabs.size() - 1);
        return highest.getExtraCharge().setScale(MONETARY_SCALE, ROUNDING_MODE);
    }
}
