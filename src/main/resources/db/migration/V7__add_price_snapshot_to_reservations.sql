ALTER TABLE reservations
    ADD COLUMN unit_price NUMERIC(12, 2) NOT NULL;

ALTER TABLE reservations
    ADD CONSTRAINT chk_reservations_unit_price
        CHECK (unit_price > 0);