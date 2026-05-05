-- =============================================================================
-- V10 — Create password_reset_tokens table
-- =============================================================================
-- Stores temporary tokens for password reset flow.
-- Each token:
--   - Is unique per request
--   - Expires after 15 minutes
--   - Is single-use (is_used = true after use)
--   - Is tied to one user email
-- =============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL,
    token       VARCHAR(255)    NOT NULL UNIQUE,
    expires_at  TIMESTAMP       NOT NULL,
    is_used     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_email   ON password_reset_tokens(email);
CREATE INDEX idx_prt_token   ON password_reset_tokens(token);
