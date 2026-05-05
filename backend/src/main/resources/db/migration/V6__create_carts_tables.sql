-- =============================================================================
-- V6 — Create carts and cart_items tables
-- =============================================================================

-- One cart per user — created at registration
CREATE TABLE IF NOT EXISTS carts (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL UNIQUE,        -- UNIQUE enforces one cart per user
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_carts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE           -- Deleting user removes their cart
);

CREATE INDEX idx_carts_user_id ON carts(user_id);

-- ─────────────────────────────────────────────────────────────────────────────

-- Individual items inside a cart
CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGSERIAL   PRIMARY KEY,
    cart_id     BIGINT      NOT NULL,
    product_id  BIGINT      NOT NULL,
    quantity    INTEGER     NOT NULL DEFAULT 1 CHECK (quantity >= 1),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- Prevent the same product appearing twice in the same cart
    -- Service layer handles this by incrementing quantity instead
    CONSTRAINT uk_cart_items_cart_product UNIQUE (cart_id, product_id),

    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id)
        REFERENCES carts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE           -- Deleted product is removed from all carts
);

CREATE INDEX idx_cart_items_cart_id    ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);
