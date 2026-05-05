package com.zenenation.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures Swagger UI / OpenAPI 3 documentation.
 *
 * Access at: http://localhost:8080/swagger-ui.html (dev only)
 * Disabled in production via application-prod.yml
 *
 * FEATURES:
 * - Full API documentation auto-generated from controllers
 * - JWT Bearer token auth built into the Swagger UI
 *   (click "Authorize" button, paste your JWT, then test protected endpoints)
 * - Dev and prod server URLs listed
 * - All endpoints grouped by controller tags
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme()));
    }

    /**
     * API metadata shown at the top of Swagger UI.
     */
    private Info apiInfo() {
        return new Info()
                .title("Zenenation E-Commerce API")
                .description("""
                        Production-grade e-commerce backend API.
                        
                        **Authentication:**
                        - Register or login to get a JWT access token
                        - Click the **Authorize** button (top right) and enter: `Bearer <your_token>`
                        - All protected endpoints will then work in Swagger UI
                        
                        **User roles:**
                        - `ROLE_USER` — browse, cart, orders
                        - `ROLE_ADMIN` — full access including dashboard
                        """)
                .version("v1.0.0")
                .contact(new Contact()
                        .name("Zenenation")
                        .email("admin@zenenation.com"))
                .license(new License()
                        .name("Private — All rights reserved"));
    }

    /**
     * Server URLs shown in Swagger UI.
     * Frontend can use these to switch between environments.
     */
    private List<Server> serverList() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort);
        devServer.setDescription("Local Development Server");

        Server stagingServer = new Server();
        stagingServer.setUrl("https://staging-api.zenenation.com");
        stagingServer.setDescription("Staging Server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.zenenation.com");
        prodServer.setDescription("Production Server");

        return List.of(devServer, stagingServer, prodServer);
    }

    /**
     * Configures JWT Bearer token as the auth scheme in Swagger UI.
     *
     * This adds an "Authorize" button to Swagger UI.
     * User pastes their JWT and all subsequent API calls
     * include "Authorization: Bearer <token>" automatically.
     */
    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT access token. Example: eyJhbGci...");
    }
}
