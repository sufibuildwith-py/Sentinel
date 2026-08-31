ALTER TABLE incident_findings
    ADD COLUMN valid_until TIMESTAMPTZ;

CREATE TABLE agent_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES revenue_incidents(incident_id),
    claim_type VARCHAR(32) NOT NULL,
    claim_text TEXT NOT NULL,
    confidence NUMERIC(5,4) NOT NULL,
    evidence_refs JSONB NOT NULL DEFAULT '[]'::jsonb,
    contradicting_evidence_refs JSONB NOT NULL DEFAULT '[]'::jsonb,
    proposed_action VARCHAR(64),
    validation_status VARCHAR(16) NOT NULL,
    validation_errors JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_claims_incident_created
    ON agent_claims(incident_id, created_at);
