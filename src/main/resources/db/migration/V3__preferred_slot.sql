-- Additive preferred-slot columns. Existing rows stay walk-in (NULL slot / booking_type).
ALTER TABLE queue_tokens ADD COLUMN IF NOT EXISTS preferred_slot_at TIMESTAMP NULL;
ALTER TABLE queue_tokens ADD COLUMN IF NOT EXISTS slot_start TIME NULL;
ALTER TABLE queue_tokens ADD COLUMN IF NOT EXISTS booking_type VARCHAR(20) NULL;

-- One live booking per doctor + date + slot. Walk-ins (slot_start NULL) are unconstrained.
CREATE UNIQUE INDEX IF NOT EXISTS ux_queue_tokens_doctor_date_slot
    ON queue_tokens (tenant_id, shop_id, doctor_id, token_date, slot_start)
    WHERE slot_start IS NOT NULL
      AND status NOT IN ('CANCELLED', 'NO_SHOW');
