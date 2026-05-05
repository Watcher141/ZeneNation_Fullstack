package com.zenenation.backend.util;

import com.zenenation.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Generates URL-friendly slugs from product names.
 *
 * WHY SLUGS?
 * Instead of /api/v1/products/42 (exposes DB IDs, not SEO-friendly)
 * we use /api/v1/products/apple-iphone-15-pro-max (readable, SEO-friendly)
 *
 * SLUG RULES:
 * - Lowercase only
 * - Spaces replaced with hyphens
 * - Special characters removed
 * - Accented characters normalized (é → e, ü → u)
 * - Multiple hyphens collapsed to one
 * - No leading or trailing hyphens
 *
 * UNIQUENESS:
 * If "iPhone 15" already exists as "iphone-15",
 * the next one becomes "iphone-15-1", then "iphone-15-2" etc.
 * This is handled by the generateUniqueSlug() method.
 *
 * Example:
 *   "Apple iPhone 15 Pro Max!" → "apple-iphone-15-pro-max"
 *   "Café Latte (Hot)"        → "cafe-latte-hot"
 */
@Component
@RequiredArgsConstructor
public class SlugUtil {

    private final ProductRepository productRepository;

    // Matches anything that is NOT a letter, digit, or hyphen
    private static final Pattern NON_SLUG_CHARS = Pattern.compile("[^a-z0-9-]");

    // Matches two or more consecutive hyphens
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");

    /**
     * Convert a raw name string into a URL-friendly slug.
     * Does NOT check for uniqueness — use generateUniqueSlug() for that.
     *
     * @param name  raw product or category name
     * @return      lowercase hyphenated slug
     */
    public static String toSlug(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        String slug = name.trim().toLowerCase();

        // Normalize accented characters: é → e, ü → u, ñ → n etc.
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = slug.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Replace spaces and underscores with hyphens
        slug = slug.replace(" ", "-").replace("_", "-");

        // Remove all characters that are not letters, digits, or hyphens
        slug = NON_SLUG_CHARS.matcher(slug).replaceAll("");

        // Collapse multiple consecutive hyphens into one
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");

        // Remove leading and trailing hyphens
        slug = slug.replaceAll("^-+|-+$", "");

        return slug;
    }

    /**
     * Generates a slug from name AND ensures it is unique in the DB.
     * If the slug already exists, appends a number suffix.
     *
     * Example:
     *   "iPhone 15" → "iphone-15"         (if not taken)
     *   "iPhone 15" → "iphone-15-1"       (if "iphone-15" taken)
     *   "iPhone 15" → "iphone-15-2"       (if both above taken)
     *
     * Called when creating a new product.
     *
     * @param name  product name
     * @return      unique slug safe to store in DB
     */
    public String generateUniqueSlug(String name) {
        String baseSlug = toSlug(name);
        String candidateSlug = baseSlug;
        int counter = 1;

        // Keep incrementing until we find a slug that doesn't exist
        while (productRepository.existsBySlug(candidateSlug)) {
            candidateSlug = baseSlug + "-" + counter;
            counter++;
        }

        return candidateSlug;
    }

    /**
     * Generates a unique slug for UPDATE operations.
     * Excludes the current product's own slug from the uniqueness check —
     * otherwise updating a product with the same name would fail.
     *
     * @param name       new product name
     * @param currentSlug the product's existing slug (to exclude from check)
     * @return           unique slug safe to store in DB
     */
    public String generateUniqueSlugForUpdate(String name, String currentSlug) {
        String baseSlug = toSlug(name);

        // If slug hasn't changed — no need to check uniqueness
        if (baseSlug.equals(currentSlug)) {
            return currentSlug;
        }

        String candidateSlug = baseSlug;
        int counter = 1;

        while (productRepository.existsBySlug(candidateSlug)) {
            candidateSlug = baseSlug + "-" + counter;
            counter++;
        }

        return candidateSlug;
    }
}
