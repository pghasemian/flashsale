ALTER TABLE reservations
    ADD COLUMN idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX uk_reservations_idempotency_key
    ON reservations(idempotency_key);