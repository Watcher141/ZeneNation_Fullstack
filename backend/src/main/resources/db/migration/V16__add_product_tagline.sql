-- =============================================================================
-- V16 — Add tagline to products
-- =============================================================================
-- Short secondary text shown below product name on detail page.
-- Examples: "Believe it! — Limited Edition", "Hand-crafted 104cm Wooden Katana"
-- =============================================================================

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS tagline VARCHAR(200);