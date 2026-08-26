-- Completed visits free the preferred slot (same as cancel / no-show).
DROP INDEX IF EXISTS ux_queue_tokens_doctor_date_slot;

CREATE UNIQUE INDEX ux_queue_tokens_doctor_date_slot
    ON queue_tokens (tenant_id, shop_id, doctor_id, token_date, slot_start)
    WHERE slot_start IS NOT NULL
      AND status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED');
