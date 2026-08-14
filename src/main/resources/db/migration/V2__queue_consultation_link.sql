-- Optional link from queue token to clinical consultation (same encounter continuity).
ALTER TABLE queue_tokens
    ADD COLUMN IF NOT EXISTS consultation_id BIGINT;

CREATE INDEX IF NOT EXISTS ix_queue_consultation
    ON queue_tokens (tenant_id, shop_id, consultation_id)
    WHERE consultation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_queue_attention
    ON queue_tokens (tenant_id, shop_id, doctor_id, status, token_date);
