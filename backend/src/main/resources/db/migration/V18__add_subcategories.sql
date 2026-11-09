-- =============================================================================
-- V18 — Subcategories (self-referencing categories)
-- =============================================================================
-- Categories can now have a parent category.
-- Top-level categories: parent_id IS NULL
-- Subcategories:        parent_id = id of parent category
--
-- Example:
--   Figures (parent_id = NULL)
--     └── Naruto Figures (parent_id = Figures.id)
--     └── One Piece Figures (parent_id = Figures.id)
-- =============================================================================

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS parent_id BIGINT,
    ADD CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);