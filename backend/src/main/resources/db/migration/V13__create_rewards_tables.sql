-- =============================================================================
-- V13 — Rewards System
-- =============================================================================
-- Rules:
--   - 20% of order total credited as reward points after DELIVERED status
--   - 1 point = ₹1 discount at checkout
--   - Points expire 1 year from credit date
--   - Ledger tracks every CREDIT and DEBIT with reason
--   - Redeemed points are locked to an order (rolled back if order cancelled)
-- =============================================================================

CREATE TABLE IF NOT EXISTS reward_ledger (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    order_id        BIGINT,                         -- linked order (nullable for manual credits)
    transaction_type VARCHAR(10)    NOT NULL,        -- 'CREDIT' or 'DEBIT'
    points          INTEGER         NOT NULL,        -- always positive
    balance_after   INTEGER         NOT NULL,        -- running balance snapshot
    reason          VARCHAR(255)    NOT NULL,        -- human-readable reason
    expires_at      TIMESTAMP,                      -- only for CREDIT entries
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_transaction_type CHECK (transaction_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_points_positive   CHECK (points > 0),

    CONSTRAINT fk_reward_ledger_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_reward_ledger_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL
);

CREATE INDEX idx_reward_ledger_user_id ON reward_ledger(user_id);
CREATE INDEX idx_reward_ledger_order_id ON reward_ledger(order_id);

-- Wallet table — current balance per user (denormalized for fast reads)
CREATE TABLE IF NOT EXISTS reward_wallet (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL UNIQUE,
    balance         INTEGER         NOT NULL DEFAULT 0,
    lifetime_earned INTEGER         NOT NULL DEFAULT 0,
    lifetime_used   INTEGER         NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),

    CONSTRAINT fk_reward_wallet_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_reward_wallet_user_id ON reward_wallet(user_id);