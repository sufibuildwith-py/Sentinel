CREATE TABLE recovery_opportunity_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    maturity VARCHAR(8) NOT NULL,
    mode VARCHAR(32) NOT NULL,
    feature_schema_version VARCHAR(32) NOT NULL,
    candidates JSONB NOT NULL,
    shadow_choice VARCHAR(64) NOT NULL,
    fallback_strategy VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_opportunity_incident_created ON recovery_opportunity_decisions(incident_id, created_at);
