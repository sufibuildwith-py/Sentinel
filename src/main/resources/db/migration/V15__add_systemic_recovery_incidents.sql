CREATE TABLE systemic_recovery_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    scope VARCHAR(128) NOT NULL,
    root_cause_candidates JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE systemic_incident_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    systemic_incident_id UUID NOT NULL REFERENCES systemic_recovery_incidents(id) ON DELETE CASCADE,
    payment_incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_systemic_member_payment_incident UNIQUE (payment_incident_id)
);

CREATE INDEX idx_systemic_incidents_merchant_created
    ON systemic_recovery_incidents(merchant_id, created_at DESC);
CREATE INDEX idx_systemic_members_parent
    ON systemic_incident_members(systemic_incident_id);
