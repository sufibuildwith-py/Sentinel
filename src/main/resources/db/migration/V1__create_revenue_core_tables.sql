CREATE TABLE payment_events (
    id UUID PRIMARY KEY,
    payment_id VARCHAR(128) NOT NULL,
    order_id VARCHAR(128) NOT NULL,
    customer_id VARCHAR(128) NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    currency VARCHAR(3) NOT NULL,
    method VARCHAR(64) NOT NULL,
    issuer_bank VARCHAR(128),
    status VARCHAR(64) NOT NULL,
    error_code VARCHAR(128),
    error_description TEXT,
    event_timestamp TIMESTAMPTZ NOT NULL,
    attempt_number INTEGER NOT NULL CHECK (attempt_number >= 1),
    previous_successful_method VARCHAR(64),
    previous_failure_count INTEGER NOT NULL CHECK (previous_failure_count >= 0),
    subscription_id VARCHAR(128),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uk_payment_events_payment_attempt UNIQUE (payment_id, attempt_number)
);

CREATE INDEX idx_payment_events_timestamp ON payment_events (event_timestamp);
CREATE INDEX idx_payment_events_customer ON payment_events (customer_id);

CREATE TABLE revenue_incidents (
    incident_id UUID PRIMARY KEY,
    type VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    amount_at_risk_minor BIGINT NOT NULL CHECK (amount_at_risk_minor >= 0),
    detected_at TIMESTAMPTZ NOT NULL,
    affected_payments JSONB NOT NULL DEFAULT '[]'::jsonb,
    affected_customers JSONB NOT NULL DEFAULT '[]'::jsonb,
    evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    root_cause TEXT,
    policy_decision VARCHAR(16)
);

CREATE INDEX idx_revenue_incidents_status ON revenue_incidents (status);
CREATE INDEX idx_revenue_incidents_detected_at ON revenue_incidents (detected_at);
