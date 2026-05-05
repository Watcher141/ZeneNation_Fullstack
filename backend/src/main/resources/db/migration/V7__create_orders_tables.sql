-- =============================================================================
-- V7 — Create orders and order_items tables
-- =============================================================================

CREATE TABLE IF NOT EXISTS orders (
    id                      BIGSERIAL       PRIMARY KEY,
    order_number            VARCHAR(30)     NOT NULL UNIQUE,    -- e.g. ORD-20240115-00000001
    user_id                 BIGINT          NOT NULL,

    -- Pricing (all stored at order time — never change)
    subtotal                NUMERIC(10, 2)  NOT NULL,
    delivery_charge         NUMERIC(10, 2)  NOT NULL DEFAULT 0.00,
    discount_amount         NUMERIC(10, 2)  NOT NULL DEFAULT 0.00,
    total_amount            NUMERIC(10, 2)  NOT NULL,

    -- Status
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    payment_method          VARCHAR(10)     NOT NULL,
    payment_status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',

    -- Delivery address snapshot (copied from addresses table at checkout)
    -- We store it here so order history is preserved even if user changes/deletes address
    delivery_name           VARCHAR(100)    NOT NULL,
    delivery_phone          VARCHAR(15)     NOT NULL,
    delivery_address_line1  VARCHAR(255)    NOT NULL,
    delivery_address_line2  VARCHAR(255),
    delivery_city           VARCHAR(100)    NOT NULL,
    delivery_state          VARCHAR(100)    NOT NULL,
    delivery_pincode        VARCHAR(10)     NOT NULL,
    delivery_country        VARCHAR(100)    NOT NULL DEFAULT 'India',

    -- Notes
    user_note               TEXT,
    admin_note              TEXT,

    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,         -- Never delete user if they have orders

    CONSTRAINT chk_orders_status
        CHECK (status IN ('PENDING','CONFIRMED','PROCESSING','SHIPPED',
                          'DELIVERED','CANCELLED','PAYMENT_FAILED','RETURNED')),

    CONSTRAINT chk_orders_payment_method
        CHECK (payment_method IN ('COD', 'ONLINE')),

    CONSTRAINT chk_orders_payment_status
        CHECK (payment_status IN ('PENDING','PAID','FAILED',
                                  'REFUND_INITIATED','REFUNDED'))
);

CREATE INDEX idx_orders_user_id        ON orders(user_id);
CREATE INDEX idx_orders_status         ON orders(status);
CREATE INDEX idx_orders_payment_method ON orders(payment_method);
CREATE INDEX idx_orders_created_at     ON orders(created_at);
CREATE INDEX idx_orders_order_number   ON orders(order_number);

-- ─────────────────────────────────────────────────────────────────────────────

-- Individual product lines inside an order
CREATE TABLE IF NOT EXISTS order_items (
    id                  BIGSERIAL       PRIMARY KEY,
    order_id            BIGINT          NOT NULL,
    product_id          BIGINT,                         -- Nullable: product may be deleted later

    -- Snapshots — captured at order time, never change
    product_name        VARCHAR(200)    NOT NULL,       -- Product name at time of purchase
    product_image_url   TEXT,                           -- Thumbnail at time of purchase
    price_at_purchase   NUMERIC(10, 2)  NOT NULL,       -- Price paid per unit
    quantity            INTEGER         NOT NULL CHECK (quantity >= 1),
    total_price         NUMERIC(10, 2)  NOT NULL,       -- price_at_purchase × quantity

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE SET NULL          -- If product deleted, keep order history (product_id = NULL)
                                    -- product_name snapshot preserves the display name
);

CREATE INDEX idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
