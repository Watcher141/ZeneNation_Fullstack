-- =============================================================================
-- V17 — Preorder System
-- =============================================================================
-- Products can be marked as preorder:
--   - Shown in dedicated preorder section on homepage
--   - Hidden from regular category browsing
--   - When admin sets available = true → moves to regular category
--
-- Preorder orders have two payment options:
--   HALF  → 50% now, 50% when shipped (remaining_amount stored)
--   FULL  → 100% upfront (standard flow)
-- =============================================================================

-- Add preorder fields to products
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS is_preorder          BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS estimated_ship_date  DATE,
    ADD COLUMN IF NOT EXISTS preorder_note        VARCHAR(300);

-- Add preorder fields to orders
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS is_preorder_order    BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS preorder_payment_type VARCHAR(10) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS remaining_amount     NUMERIC(10,2) DEFAULT NULL;

-- Constraint on payment type
ALTER TABLE orders
    ADD CONSTRAINT chk_preorder_payment_type
        CHECK (preorder_payment_type IN ('HALF', 'FULL') OR preorder_payment_type IS NULL);

CREATE INDEX IF NOT EXISTS idx_products_is_preorder ON products(is_preorder);