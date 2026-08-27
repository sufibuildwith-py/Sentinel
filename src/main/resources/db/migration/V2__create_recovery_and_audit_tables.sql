CREATE TABLE incident_findings (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES revenue_incidents (incident_id),
    source VARCHAR(32) NOT NULL,
    summary TEXT NOT NULL,
    confidence NUMERIC(5, 4) CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_incident_findings_incident ON incident_findings (incident_id);

CREATE TABLE recovery_plans (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES revenue_incidents (incident_id),
    strategy VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    target_payment_count INTEGER NOT NULL CHECK (target_payment_count >= 0),
    target_amount_minor BIGINT NOT NULL CHECK (target_amount_minor >= 0),
    confidence NUMERIC(5, 4) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    estimated_recovery_minor BIGINT NOT NULL CHECK (estimated_recovery_minor >= 0),
    risk_level VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_recovery_plans_incident ON recovery_plans (incident_id);

CREATE TABLE recovery_actions (
    id UUID PRIMARY KEY,
    recovery_plan_id UUID NOT NULL REFERENCES recovery_plans (id),
    incident_id UUID NOT NULL REFERENCES revenue_incidents (incident_id),
    status VARCHAR(32) NOT NULL,
    policy_decision VARCHAR(16),
    external_resource_type VARCHAR(64),
    external_resource_id VARCHAR(128),
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    approved_at TIMESTAMPTZ,
    executed_at TIMESTAMPTZ
);

CREATE INDEX idx_recovery_actions_plan ON recovery_actions (recovery_plan_id);
CREATE INDEX idx_recovery_actions_incident ON recovery_actions (incident_id);

CREATE TABLE processed_webhooks (
    id UUID PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    raw_payload JSONB NOT NULL,
    signature_valid BOOLEAN NOT NULL,
    CONSTRAINT uk_processed_webhooks_event_id UNIQUE (event_id)
);

CREATE TABLE recovery_outcomes (
    id UUID PRIMARY KEY,
    recovery_action_id UUID NOT NULL REFERENCES recovery_actions (id),
    incident_id UUID NOT NULL REFERENCES revenue_incidents (incident_id),
    status VARCHAR(32) NOT NULL,
    recovered_amount_minor BIGINT NOT NULL CHECK (recovered_amount_minor >= 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    source_event_id VARCHAR(128),
    CONSTRAINT uk_recovery_outcomes_action UNIQUE (recovery_action_id)
);

CREATE INDEX idx_recovery_outcomes_incident ON recovery_outcomes (incident_id);
CREATE INDEX idx_recovery_outcomes_source_event ON recovery_outcomes (source_event_id);

CREATE TABLE audit_events (
    event_id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES revenue_incidents (incident_id),
    timestamp TIMESTAMPTZ NOT NULL,
    actor VARCHAR(128) NOT NULL,
    agent VARCHAR(128),
    action VARCHAR(128) NOT NULL,
    input_evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    confidence NUMERIC(5, 4) CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
    decision TEXT,
    policy_rules_evaluated JSONB NOT NULL DEFAULT '[]'::jsonb,
    policy_result VARCHAR(64),
    external_resource_id VARCHAR(128),
    previous_state VARCHAR(32),
    new_state VARCHAR(32),
    outcome TEXT
);

CREATE INDEX idx_audit_events_incident_timestamp ON audit_events (incident_id, timestamp);

CREATE TABLE historical_incidents (
    id UUID PRIMARY KEY,
    original_incident_id UUID REFERENCES revenue_incidents (incident_id) ON DELETE SET NULL,
    root_cause TEXT NOT NULL,
    evidence_summary JSONB NOT NULL,
    recovery_strategy VARCHAR(64) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    recovered_amount_minor BIGINT NOT NULL CHECK (recovered_amount_minor >= 0),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_historical_incidents_original ON historical_incidents (original_incident_id);
CREATE INDEX idx_historical_incidents_strategy ON historical_incidents (recovery_strategy);
