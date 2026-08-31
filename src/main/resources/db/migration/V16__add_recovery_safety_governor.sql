CREATE TABLE recovery_governor_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    recovery_action_id UUID NOT NULL REFERENCES recovery_actions(id),
    allowed BOOLEAN NOT NULL,
    allowed_value_minor BIGINT NOT NULL,
    envelope JSONB NOT NULL,
    violations JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE recovery_kill_switches (
    switch_name VARCHAR(64) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    reason TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO recovery_kill_switches(switch_name) VALUES
 ('ALL_AUTONOMOUS_EXECUTION'), ('PAYMENT_LINK_CREATION'), ('NEW_ORDER_CREATION'),
 ('CUSTOMER_OUTREACH'), ('MODEL_DRIVEN_RANKING');

CREATE TABLE recovery_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    strategy VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    incident_ids JSONB NOT NULL,
    canary_size INT NOT NULL,
    released_count INT NOT NULL DEFAULT 0,
    required_reconciled_count INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_governor_decisions_incident ON recovery_governor_decisions(incident_id, created_at);
CREATE INDEX idx_recovery_batches_status ON recovery_batches(status);
