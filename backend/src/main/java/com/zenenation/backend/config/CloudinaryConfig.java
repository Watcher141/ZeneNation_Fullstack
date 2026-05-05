package com.zenenation.backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes and exposes the Cloudinary SDK as a Spring bean.
 *
 * Cloudinary credentials are read from environment variables via application.yml:
 *   cloudinary.cloud-name   → CLOUDINARY_CLOUD_NAME
 *   cloudinary.api-key      → CLOUDINARY_API_KEY
 *   cloudinary.api-secret   → CLOUDINARY_API_SECRET
 *
 * The Cloudinary bean is injected into CloudinaryService
 * which handles all upload/delete operations.
 *
 * NEVER hardcode Cloudinary credentials here.
 * Always use environment variables in production.
 */
@Configuration
@Slf4j
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        log.info("Initializing Cloudinary with cloud name: {}", cloudName);

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true    // Always use HTTPS URLs — never HTTP
        ));

        log.info("Cloudinary initialized successfully");
        return cloudinary;
    }
}
