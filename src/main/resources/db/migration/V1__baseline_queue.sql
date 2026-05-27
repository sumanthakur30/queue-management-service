CREATE TABLE IF NOT EXISTS queue_tokens (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    shop_id VARCHAR(100) NOT NULL,
    branch_id BIGINT NOT NULL,
    appointment_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    token_number INT NOT NULL,
    token_date DATE NOT NULL,
    queue_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(30) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    checked_in_at TIMESTAMP NOT NULL,
    called_at TIMESTAMP,
    consult_started_at TIMESTAMP,
    completed_at TIMESTAMP,
    estimated_wait_minutes INT,
    opd_room VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (tenant_id, shop_id, doctor_id, token_date, token_number)
);

CREATE INDEX IF NOT EXISTS ix_queue_live
    ON queue_tokens (tenant_id, shop_id, doctor_id, token_date, status);
