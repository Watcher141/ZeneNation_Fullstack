-- =============================================================================
-- V1 — Create users table
-- =============================================================================
-- Flyway naming convention: V{version}__{description}.sql
-- Double underscore between version and description is REQUIRED by Flyway.
-- Scripts run in version order: V1, V2, V3...
-- Once applied, a script is NEVER run again (Flyway tracks via flyway_schema_history).
-- NEVER edit an already-applied migration — create a new one instead.
-- =============================================================================

CREATE TABLE IF NOT EXISTS users (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    email               VARCHAR(255)    NOT NULL UNIQUE,
    password            VARCHAR(255),                           -- NULL for OAuth2 users
    phone_number        VARCHAR(15),
    role                VARCHAR(20)     NOT NULL DEFAULT 'ROLE_USER',
    provider            VARCHAR(20)     NOT NULL DEFAULT 'LOCAL',
    provider_id         VARCHAR(255),                           -- Google user ID
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    is_email_verified   BOOLEAN         NOT NULL DEFAULT FALSE,
    profile_image_url   TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Indexes for fast lookups
CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_provider ON users(provider);

-- Constraint: role must be one of our valid values
ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'));

-- Constraint: provider must be one of our valid values
ALTER TABLE users ADD CONSTRAINT chk_users_provider
    CHECK (provider IN ('LOCAL', 'GOOGLE'));
