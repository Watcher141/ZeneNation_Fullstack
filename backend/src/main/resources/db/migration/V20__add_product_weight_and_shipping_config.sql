-- =============================================================================
-- V20: Add product weight + shipping config tables (delivery & COD slabs)
-- =============================================================================

-- ── 1. Add weight column to products ─────────────────────────────────────────
ALTER TABLE products ADD COLUMN weight_grams INTEGER NOT NULL DEFAULT 0;

-- ── 2. Add COD charge column to orders ───────────────────────────────────────
ALTER TABLE orders ADD COLUMN cod_charge NUMERIC(10,2) NOT NULL DEFAULT 0.00;

-- ── 3. Delivery charge slabs (weight-based) ──────────────────────────────────
CREATE TABLE delivery_charge_slabs (
    id              BIGSERIAL    PRIMARY KEY,
    min_weight_grams INTEGER     NOT NULL,
    max_weight_grams INTEGER     NOT NULL,
    charge          NUMERIC(10,2) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_delivery_slab_range UNIQUE (min_weight_grams, max_weight_grams)
);

-- ── 4. COD charge slabs (order-amount-based) ─────────────────────────────────
CREATE TABLE cod_charge_slabs (
    id               BIGSERIAL    PRIMARY KEY,
    min_order_amount NUMERIC(10,2) NOT NULL,
    max_order_amount NUMERIC(10,2) NOT NULL,
    extra_charge     NUMERIC(10,2) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cod_slab_range UNIQUE (min_order_amount, max_order_amount)
);

-- ── 5. Seed delivery charge slabs ────────────────────────────────────────────
INSERT INTO delivery_charge_slabs (min_weight_grams, max_weight_grams, charge) VALUES
    (1,    250,   59.00),
    (251,  500,   69.00),
    (501,  1000,  99.00),
    (1001, 2000,  149.00),
    (2001, 3000,  199.00),
    (3001, 4000,  249.00),
    (4001, 5000,  299.00),
    (5001, 6000,  339.00),
    (6001, 7000,  379.00),
    (7001, 8000,  419.00),
    (8001, 9000,  459.00),
    (9001, 10000, 500.00);

-- ── 6. Seed COD charge slabs ─────────────────────────────────────────────────
INSERT INTO cod_charge_slabs (min_order_amount, max_order_amount, extra_charge) VALUES
    (0.00,    499.99,  100.00),
    (500.00,  999.99,  150.00),
    (1000.00, 1499.99, 200.00),
    (1500.00, 1999.99, 250.00),
    (2000.00, 2499.99, 300.00),
    (2500.00, 2999.99, 350.00),
    (3000.00, 3499.99, 400.00),
    (3500.00, 3999.99, 450.00),
    (4000.00, 4499.99, 500.00);
