-- V19 — Home Sections (admin-managed homepage sections)

CREATE TABLE IF NOT EXISTS home_sections (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(100) NOT NULL,
    subtitle        VARCHAR(255),
    type            VARCHAR(30)  NOT NULL DEFAULT 'CUSTOM',
    display_order   INT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    view_all_url    VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS home_section_products (
    id              BIGSERIAL PRIMARY KEY,
    section_id      BIGINT NOT NULL REFERENCES home_sections(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    display_order   INT    NOT NULL DEFAULT 0,
    UNIQUE(section_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_home_sections_active ON home_sections(is_active, display_order);
CREATE INDEX IF NOT EXISTS idx_home_section_products_section ON home_section_products(section_id, display_order);

-- Seed default sections
INSERT INTO home_sections (title, subtitle, type, display_order, is_active, view_all_url) VALUES
  ('New Arrivals',        'Latest additions to our collection',            'NEW_ARRIVAL', 1, true, '/products'),
  ('Preorder Collection', 'Reserve upcoming anime collectibles',           'PREORDER',    2, true, '/preorder'),
  ('Featured Products',   'Hand-picked favourites from our team',          'CUSTOM',      3, true, '/products')
ON CONFLICT DO NOTHING;