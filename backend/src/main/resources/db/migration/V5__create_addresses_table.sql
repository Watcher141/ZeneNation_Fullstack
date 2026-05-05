-- =============================================================================
-- V5 — Create addresses table
-- =============================================================================

CREATE TABLE IF NOT EXISTS addresses (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,           -- Recipient name
    phone_number    VARCHAR(15)     NOT NULL,
    address_line1   VARCHAR(255)    NOT NULL,
    address_line2   VARCHAR(255),
    city            VARCHAR(100)    NOT NULL,
    state           VARCHAR(100)    NOT NULL,
    pincode         VARCHAR(10)     NOT NULL,
    country         VARCHAR(100)    NOT NULL DEFAULT 'India',
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_addresses_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE           -- Deleting user removes all their addresses
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);
