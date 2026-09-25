CREATE TABLE skus (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id),
    sku_code VARCHAR(100) NOT NULL UNIQUE,
    price NUMERIC(12,2) NOT NULL,
    attributes VARCHAR(500)
);

CREATE INDEX idx_skus_product_id ON skus (product_id);
