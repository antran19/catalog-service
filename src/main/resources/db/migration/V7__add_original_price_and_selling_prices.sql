-- Three prices (price model). skus.price stays and now means the SELLING price: the price a
-- buyer sees and the one commerce-service puts on a cart. skus.original_price is the cost price
-- of the goods when stocked. It is never returned to buyers, and a selling price below it is
-- refused. Every selling price ever set is kept in product_prices.
ALTER TABLE skus ADD COLUMN original_price NUMERIC(12,2);

-- Existing SKUs were priced once: that price is both their original and their selling price.
UPDATE skus SET original_price = price;

ALTER TABLE skus ALTER COLUMN original_price SET NOT NULL;

ALTER TABLE skus
    ADD CONSTRAINT chk_skus_original_price_positive CHECK (original_price > 0),
    ADD CONSTRAINT chk_skus_selling_not_below_original CHECK (price >= original_price);

CREATE TABLE product_prices (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id         UUID NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    price          NUMERIC(12,2) NOT NULL CHECK (price > 0),
    effective_from TIMESTAMPTZ NOT NULL DEFAULT now(),
    set_by         VARCHAR(255)
);

CREATE INDEX idx_product_prices_sku_effective ON product_prices (sku_id, effective_from DESC);

-- History starts with the price each existing SKU already has.
INSERT INTO product_prices (sku_id, price, effective_from)
SELECT id, price, now() FROM skus;
