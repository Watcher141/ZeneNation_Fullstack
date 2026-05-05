-- =============================================================================
-- V14 — Email Subscribers + Announcements
-- =============================================================================

-- Email subscribers (newsletter popup)
CREATE TABLE IF NOT EXISTS email_subscribers (
    id           BIGSERIAL    PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    name         VARCHAR(100),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    unsubscribed_at TIMESTAMP
);

CREATE INDEX idx_subscribers_email    ON email_subscribers(email);
CREATE INDEX idx_subscribers_active   ON email_subscribers(is_active);

-- Announcements (admin creates, shown as banner + email blast)
CREATE TABLE IF NOT EXISTS announcements (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    message      TEXT         NOT NULL,
    type         VARCHAR(20)  NOT NULL DEFAULT 'INFO',   -- INFO, DEAL, WARNING, SUCCESS
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,     -- show on website
    email_sent   BOOLEAN      NOT NULL DEFAULT FALSE,    -- was email blast sent?
    starts_at    TIMESTAMP,
    ends_at      TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_announcement_type CHECK (type IN ('INFO','DEAL','WARNING','SUCCESS'))
);

CREATE TRIGGER set_updated_at_announcements
    BEFORE UPDATE ON announcements
    FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();
