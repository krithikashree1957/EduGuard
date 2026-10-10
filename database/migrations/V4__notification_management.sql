BEGIN;

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipient_reference VARCHAR(150) NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('EMAIL', 'IN_APP')),
    event_type VARCHAR(80) NOT NULL,
    template_id VARCHAR(80) NOT NULL,
    subject VARCHAR(200),
    message VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMP,
    delivered_at TIMESTAMP,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    error_summary VARCHAR(500),
    idempotency_key VARCHAR(200) UNIQUE,
    delivery_mode VARCHAR(20) NOT NULL,
    read_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient
    ON notifications (recipient_reference, created_at DESC, notification_id DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_status
    ON notifications (status);

CREATE TABLE IF NOT EXISTS notification_attempts (
    attempt_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES notifications(notification_id),
    outcome VARCHAR(20) NOT NULL,
    error_summary VARCHAR(500),
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_attempts_notification
    ON notification_attempts (notification_id, attempted_at ASC, attempt_id ASC);

COMMIT;
