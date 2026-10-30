-- ProductStatus is an enum in Java, but the column is a plain VARCHAR(20): a row written any other
-- way (a script, a migration, a future service) could hold a status nobody knows, or a SKU priced
-- at zero. These constraints make the database refuse such rows too.
ALTER TABLE products
    ADD CONSTRAINT chk_products_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'SOLD'));

ALTER TABLE skus
    ADD CONSTRAINT chk_skus_price_positive
        CHECK (price > 0);
