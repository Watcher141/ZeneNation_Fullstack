-- =============================================================================
-- V9 — Auto-update updated_at timestamp on every row change
-- =============================================================================
-- Without this, updated_at only changes if Hibernate explicitly sets it.
-- With this trigger, PostgreSQL handles it at the DB level automatically —
-- even if someone updates a row via direct SQL (admin ops, hotfixes etc.)
-- =============================================================================

-- Create a reusable trigger function
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables that have an updated_at column

CREATE TRIGGER set_updated_at_users
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_categories
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_products
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_addresses
    BEFORE UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_cart_items
    BEFORE UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_orders
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_payments
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
