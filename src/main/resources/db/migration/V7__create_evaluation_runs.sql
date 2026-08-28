CREATE TABLE evaluation_runs (
    id UUID PRIMARY KEY,
    report_version VARCHAR(32) NOT NULL,
    seed BIGINT NOT NULL,
    dataset_size INTEGER NOT NULL CHECK (dataset_size BETWEEN 300 AND 500),
    report_sha256 VARCHAR(64) NOT NULL,
    report_json JSONB NOT NULL,
    report_markdown TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (report_version, seed, report_sha256)
);

CREATE INDEX idx_evaluation_runs_created_at ON evaluation_runs (created_at DESC);
