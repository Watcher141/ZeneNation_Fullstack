package com.zenenation.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures Caffeine in-memory caching.
 *
 * WHY CACHE?
 * Product listings and category lists are read far more often than they change.
 * Without cache: every /api/v1/products request hits the database.
 * With cache: first request hits DB, subsequent requests served from memory
 *             until cache expires or is evicted (on admin update).
 *
 * CACHE NAMES — used with @Cacheable annotations in services:
 *
 *   "categories"       → all active categories list (rarely changes)
 *   "category"         → single category by ID
 *   "products"         → paginated product listings
 *   "product"          → single product by ID or slug
 *
 * CACHE INVALIDATION — when admin updates/deletes a product or category,
 * we call @CacheEvict to remove stale data. This is handled in the
 * service layer automatically.
 *
 * TTL (Time To Live) — cache entries expire after 10 minutes even if
 * not evicted. This ensures stale data never lives longer than 10 minutes
 * in the worst case (e.g., if eviction was missed somehow).
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    // Cache names — defined as constants to avoid typos across the codebase
    public static final String CACHE_CATEGORIES    = "categories";
    public static final String CACHE_CATEGORY      = "category";
    public static final String CACHE_PRODUCTS      = "products";
    public static final String CACHE_PRODUCT       = "product";
    public static final String CACHE_DASHBOARD     = "dashboard";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_CATEGORIES,
                CACHE_CATEGORY,
                CACHE_PRODUCTS,
                CACHE_PRODUCT,
                CACHE_DASHBOARD
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());
        cacheManager.setAllowNullValues(false); // Never cache null — forces re-fetch

        log.info("Caffeine cache manager initialized with caches: categories, category, products, product, dashboard");
        return cacheManager;
    }

    /**
     * Caffeine cache configuration:
     *
     * expireAfterWrite(10, MINUTES)
     *   → Entry expires 10 minutes after it was written.
     *   → Guarantees max staleness of 10 minutes even without explicit eviction.
     *
     * maximumSize(500)
     *   → Never hold more than 500 entries per cache.
     *   → Protects against unbounded memory growth.
     *   → Caffeine evicts least-recently-used entries when limit is reached.
     *
     * recordStats()
     *   → Enables cache hit/miss statistics.
     *   → Accessible via Spring Actuator /actuator/metrics/cache.*
     *   → Useful to verify the cache is actually being used.
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
    }
}
