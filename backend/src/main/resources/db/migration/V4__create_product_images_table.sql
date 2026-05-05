-- =============================================================================
-- V4 — Create product_images table
-- =============================================================================

CREATE TABLE IF NOT EXISTS product_images (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    image_url       TEXT            NOT NULL,
    image_public_id VARCHAR(255)    NOT NULL,       -- Required for Cloudinary deletion
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    display_order   INTEGER         NOT NULL DEFAULT 0,
    alt_text        VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE CASCADE           -- Deleting a product removes all its images from DB
                                    -- (Cloudinary cleanup done in service before this)
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);
