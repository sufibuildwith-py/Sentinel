ALTER TABLE payment_events
    ADD COLUMN reset_at TIMESTAMPTZ;

ALTER TABLE revenue_incidents
    ADD COLUMN reset_at TIMESTAMPTZ;

ALTER TABLE payment_events
    DROP CONSTRAINT uk_payment_events_payment_attempt;

CREATE UNIQUE INDEX uk_payment_events_payment_attempt_active
    ON payment_events (payment_id, attempt_number)
    WHERE reset_at IS NULL;

CREATE INDEX idx_payment_events_active
    ON payment_events (event_timestamp)
    WHERE reset_at IS NULL;

CREATE INDEX idx_revenue_incidents_active
    ON revenue_incidents (detected_at DESC)
    WHERE reset_at IS NULL;
