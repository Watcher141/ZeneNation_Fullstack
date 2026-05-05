package com.zenenation.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Binds all properties under "app:" in application.yml to this class.
 *
 * WHY @ConfigurationProperties instead of @Value?
 * - Groups related properties together in one place
 * - Type-safe (no string casting)
 * - Validates at startup — if a required property is missing, app won't start
 * - Easier to test (just instantiate and set fields)
 *
 * Usage anywhere in the app:
 *   @Autowired AppProperties appProperties;
 *   appProperties.getCors().getAllowedOrigins()
 *   appProperties.getPagination().getDefaultPageSize()
 */
@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Cors cors = new Cors();
    private OAuth2 oauth2 = new OAuth2();
    private Admin admin = new Admin();
    private Pagination pagination = new Pagination();
    private Order order = new Order();

    // -------------------------------------------------------------------------

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }

    @Getter
    @Setter
    public static class OAuth2 {
        /**
         * Frontend URL to redirect to after successful Google login.
         * Example: http://localhost:3000/oauth2/callback
         */
        private String redirectUri = "http://localhost:3000/oauth2/callback";
    }

    @Getter
    @Setter
    public static class Admin {
        /**
         * Admin account created on first startup if it doesn't exist.
         * Set via environment variables in production — never hardcode.
         */
        private String email = "admin@ecommerce.com";
        private String password = "Admin@123";
    }

    @Getter
    @Setter
    public static class Pagination {
        /**
         * Default number of items per page for all list endpoints.
         * 12 works well for product grids (3x4 or 4x3 layout).
         */
        private int defaultPageSize = 12;

        /**
         * Hard ceiling — no endpoint returns more than this in one call.
         * Protects against clients requesting 10,000 items at once.
         */
        private int maxPageSize = 50;
    }

    @Getter
    @Setter
    public static class Order {
        /**
         * Maximum order value allowed for Cash on Delivery.
         * Orders above this must use online payment.
         * Reduces financial risk on undelivered COD orders.
         */
        private int codMaxAmount = 10000;
    }
}
