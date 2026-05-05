-- =============================================================================
-- V8 — Create payments table
-- =============================================================================

CREATE TABLE IF NOT EXISTS payments (
    id                      BIGSERIAL       PRIMARY KEY,
    order_id                BIGINT          NOT NULL UNIQUE,    -- One payment per order

    -- Razorpay IDs (NULL for COD until delivered)
    razorpay_order_id       VARCHAR(100),
    razorpay_payment_id     VARCHAR(100),
    razorpay_signature      VARCHAR(255),   -- Stored for audit trail only

    -- Payment details
    amount                  NUMERIC(10, 2)  NOT NULL,
    currency                VARCHAR(3)      NOT NULL DEFAULT 'INR',
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Refund tracking
    refund_id               VARCHAR(100),
    failure_reason          TEXT,           -- Populated if payment fails

    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_payments_status
        CHECK (status IN ('PENDING','PAID','FAILED','REFUND_INITIATED','REFUNDED'))
);

-- Fast lookups by Razorpay IDs (used in webhook + verification handlers)
CREATE INDEX idx_payments_order_id              ON payments(order_id);
CREATE INDEX idx_payments_razorpay_order_id     ON payments(razorpay_order_id);
CREATE INDEX idx_payments_razorpay_payment_id   ON payments(razorpay_payment_id);
