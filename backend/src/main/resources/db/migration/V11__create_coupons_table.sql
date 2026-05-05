-- =============================================================================
-- V11 — Create coupons table
-- =============================================================================

CREATE TABLE IF NOT EXISTS coupons (
    id                  BIGSERIAL       PRIMARY KEY,
    code                VARCHAR(50)     NOT NULL UNIQUE,    -- e.g. SAVE20, FLAT100
    description         VARCHAR(255),

    -- Discount type: PERCENTAGE (20% off) or FLAT (₹100 off)
    discount_type       VARCHAR(15)     NOT NULL,
    discount_value      NUMERIC(10, 2)  NOT NULL,           -- 20.00 for 20% or 100.00 for ₹100

    -- Constraints
    minimum_order_amount NUMERIC(10, 2) NOT NULL DEFAULT 0, -- Min order to apply coupon
    maximum_discount     NUMERIC(10, 2),                    -- Cap for PERCENTAGE coupons
                                                            -- e.g. max ₹500 off even if 20% = ₹700

    -- Usage limits
    usage_limit         INTEGER,                            -- NULL = unlimited uses
    used_count          INTEGER         NOT NULL DEFAULT 0, -- How many times used so far
    per_user_limit      INTEGER         NOT NULL DEFAULT 1, -- Max uses per user

    -- Validity
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    valid_from          TIMESTAMP       NOT NULL DEFAULT NOW(),
    valid_until         TIMESTAMP,                          -- NULL = no expiry

    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_coupon_discount_type
        CHECK (discount_type IN ('PERCENTAGE', 'FLAT')),

    CONSTRAINT chk_coupon_discount_value
        CHECK (discount_value > 0),

    CONSTRAINT chk_coupon_percentage_max
        CHECK (discount_type != 'PERCENTAGE' OR discount_value <= 100)
);

CREATE INDEX idx_coupons_code      ON coupons(code);
CREATE INDEX idx_coupons_is_active ON coupons(is_active);

-- ─────────────────────────────────────────────────────────────────────────────
-- Track which user used which coupon (for per-user limit enforcement)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS coupon_usages (
    id          BIGSERIAL   PRIMARY KEY,
    coupon_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    order_id    BIGINT      NOT NULL,
    used_at     TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_coupon_usages_coupon
        FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_usages_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_usages_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_coupon_usages_coupon_id ON coupon_usages(coupon_id);
CREATE INDEX idx_coupon_usages_user_id   ON coupon_usages(user_id);

-- Trigger to auto-update updated_at on coupons
CREATE TRIGGER set_updated_at_coupons
    BEFORE UPDATE ON coupons
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
