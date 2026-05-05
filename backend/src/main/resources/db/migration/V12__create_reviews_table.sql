-- =============================================================================
-- V12 — Create product reviews table
-- =============================================================================
-- Rules:
--   - Only users who have DELIVERED orders containing the product can review
--   - One review per user per product
--   - Rating: 1 to 5 stars
--   - Admin can delete any review
-- =============================================================================

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGSERIAL       PRIMARY KEY,
    product_id  BIGINT          NOT NULL,
    user_id     BIGINT          NOT NULL,
    rating      INTEGER         NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title       VARCHAR(150),
    body        TEXT,
    is_verified BOOLEAN         NOT NULL DEFAULT FALSE,  -- true if user purchased the product
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- One review per user per product
    CONSTRAINT uk_reviews_user_product UNIQUE (user_id, product_id),

    CONSTRAINT fk_reviews_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,

    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id    ON reviews(user_id);
CREATE INDEX idx_reviews_rating     ON reviews(rating);

CREATE TRIGGER set_updated_at_reviews
    BEFORE UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();