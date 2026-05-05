-- =============================================================================
-- V3 — Create products table
-- =============================================================================

CREATE TABLE IF NOT EXISTS products (
    id               BIGSERIAL           PRIMARY KEY,
    name             VARCHAR(200)        NOT NULL,
    description      TEXT                NOT NULL,

    -- NUMERIC(10,2) = up to 99,999,999.99 — correct type for money
    -- Never use FLOAT or DOUBLE for monetary values (rounding errors)
    price            NUMERIC(10, 2)      NOT NULL CHECK (price >= 0),
    discount_percent NUMERIC(5, 2)       NOT NULL DEFAULT 0.00
                                         CHECK (discount_percent >= 0 AND discount_percent <= 100),
    stock_quantity   INTEGER             NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),

    slug             VARCHAR(255)        UNIQUE,             -- SEO-friendly URL identifier
    is_deleted       BOOLEAN             NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN             NOT NULL DEFAULT TRUE,

    -- Foreign key: every product must belong to a category
    category_id      BIGINT              NOT NULL,

    created_at       TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP           NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE RESTRICT                  -- Prevent category deletion if products exist
);

CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_is_deleted  ON products(is_deleted);
CREATE INDEX idx_products_is_active   ON products(is_active);
CREATE INDEX idx_products_name        ON products(name);
CREATE INDEX idx_products_slug        ON products(slug);
