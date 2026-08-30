CREATE TABLE payment_downtimes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    razorpay_id VARCHAR(255),
    method VARCHAR(100),
    instrument VARCHAR(100),
    begin_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    status VARCHAR(50),
    raw_payload JSONB,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_downtimes_status ON payment_downtimes(status);
CREATE INDEX idx_payment_downtimes_begin_at ON payment_downtimes(begin_at);
