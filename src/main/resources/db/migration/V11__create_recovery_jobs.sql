CREATE TABLE recovery_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL,
    policy_decision_id UUID,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    strategy VARCHAR(100),
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    last_attempted_at TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_detail TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recovery_jobs_incident_id ON recovery_jobs(incident_id);
CREATE INDEX idx_recovery_jobs_status ON recovery_jobs(status);
CREATE INDEX idx_recovery_jobs_next_attempt
    ON recovery_jobs(next_attempt_at) WHERE status = 'PENDING';
