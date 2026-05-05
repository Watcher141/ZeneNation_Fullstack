package com.zenenation.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables:
 * - @Async  → background thread execution (used for email sending)
 * - @Scheduled → periodic tasks (used for token cleanup)
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // Spring handles thread pool automatically
    // For production: configure a custom ThreadPoolTaskExecutor here
}
