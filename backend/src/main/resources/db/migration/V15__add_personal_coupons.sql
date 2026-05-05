-- =============================================================================
-- V15 — Personal Welcome Coupons (idempotent version)
-- =============================================================================

ALTER TABLE coupons
    ADD COLUMN IF NOT EXISTS assigned_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS is_welcome_coupon BOOLEAN NOT NULL DEFAULT FALSE;

-- Add FK only if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_coupon_assigned_user'
    ) THEN
        ALTER TABLE coupons
            ADD CONSTRAINT fk_coupon_assigned_user
                FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Add index only if it doesn't already exist
CREATE INDEX IF NOT EXISTS idx_coupons_assigned_user ON coupons(assigned_user_id);