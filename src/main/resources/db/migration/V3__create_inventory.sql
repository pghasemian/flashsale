-- Inventory table
CREATE TABLE inventory (
                           campaign_id BIGINT NOT NULL,
                           product_id BIGINT NOT NULL,

                           total_stock INTEGER NOT NULL DEFAULT 0,
                           reserved_stock INTEGER NOT NULL DEFAULT 0,

                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           PRIMARY KEY (campaign_id, product_id),

                           CONSTRAINT fk_inventory_campaign_product
                               FOREIGN KEY (campaign_id, product_id)
                                   REFERENCES campaign_products(campaign_id, product_id)
                                   ON DELETE CASCADE,

                           CONSTRAINT chk_inventory_total_stock
                               CHECK (total_stock >= 0),

                           CONSTRAINT chk_inventory_reserved_stock
                               CHECK (reserved_stock >= 0),

                           CONSTRAINT chk_inventory_stock_limit
                               CHECK (reserved_stock <= total_stock)
);