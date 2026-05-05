-- =============================================================================
-- V2 — Create categories table
-- =============================================================================

CREATE TABLE IF NOT EXISTS categories (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL UNIQUE,
    description     TEXT,
    image_url       TEXT,
    image_public_id VARCHAR(255),                       -- Cloudinary public ID for deletion
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_name       ON categories(name);
CREATE INDEX idx_categories_is_deleted ON categories(is_deleted);
