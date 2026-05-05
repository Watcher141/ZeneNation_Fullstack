package com.zenenation.backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates human-readable order numbers.
 *
 * FORMAT: ORD-YYYYMMDD-XXXXXXXX
 *
 * Examples:
 *   ORD-20240115-00000001
 *   ORD-20240115-00000042
 *   ORD-20241231-00001337
 *
 * WHY NOT just use the DB auto-increment ID?
 * - Exposes how many orders you have (competitors can see your volume)
 * - Not human-friendly for customer support ("your order ID is 42")
 * - Date prefix helps admin filter/sort orders by date visually
 *
 * THREAD SAFETY:
 * AtomicLong ensures no two threads generate the same sequence number
 * within the same JVM instance.
 *
 * NOTE: For a distributed system (multiple app instances),
 * replace this with a DB sequence or UUID-based order number.
 * For a single-instance deployment this is perfectly safe.
 */
public class OrderNumberUtil {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final AtomicLong sequence = new AtomicLong(0);

    // Private constructor — static utility class, never instantiate
    private OrderNumberUtil() {}

    /**
     * Generate a unique order number.
     *
     * @return order number string, e.g. "ORD-20240115-00000001"
     */
    public static String generate() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        long seq = sequence.incrementAndGet();
        return String.format("ORD-%s-%08d", datePart, seq);
    }
}
