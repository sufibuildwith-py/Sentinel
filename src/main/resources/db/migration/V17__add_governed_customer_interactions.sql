CREATE TABLE customer_contact_preferences (
    customer_ref VARCHAR(128) PRIMARY KEY,
    consent_granted BOOLEAN NOT NULL DEFAULT FALSE,
    do_not_contact BOOLEAN NOT NULL DEFAULT FALSE,
    opted_out BOOLEAN NOT NULL DEFAULT FALSE,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE customer_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    recovery_action_id UUID REFERENCES recovery_actions(id),
    customer_ref VARCHAR(128) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    intent VARCHAR(32) NOT NULL,
    template_id VARCHAR(64) NOT NULL,
    delivery_mode VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    policy_trace JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE promises_to_pay (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    recovery_action_id UUID REFERENCES recovery_actions(id),
    customer_ref VARCHAR(128) NOT NULL,
    promised_amount_minor BIGINT NOT NULL,
    balance_minor BIGINT NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    fulfilled_amount_minor BIGINT NOT NULL DEFAULT 0,
    source_event_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_interactions_customer_created ON customer_interactions(customer_ref, created_at DESC);
CREATE INDEX idx_promises_customer_status ON promises_to_pay(customer_ref, status);
CREATE INDEX idx_promises_due ON promises_to_pay(due_at) WHERE status IN ('PENDING','REMINDER_DUE');
