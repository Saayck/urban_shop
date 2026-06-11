ALTER TABLE customers
    DROP CONSTRAINT uq_customer_email_per_tenant,
    ADD CONSTRAINT ck_customers_first_name_not_blank
        CHECK (BTRIM(first_name) <> ''),
    ADD CONSTRAINT ck_customers_last_name_not_blank
        CHECK (BTRIM(last_name) <> ''),
    ADD CONSTRAINT ck_customers_email_not_blank
        CHECK (email IS NULL OR BTRIM(email) <> ''),
    ADD CONSTRAINT ck_customers_phone
        CHECK (phone ~ '^9[0-9]{8}$'),
    ADD CONSTRAINT ck_customers_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
    ADD CONSTRAINT ck_customers_document_pair
        CHECK (
            (document_type IS NULL AND document_number IS NULL)
            OR (document_type IS NOT NULL AND document_number IS NOT NULL)
        ),
    ADD CONSTRAINT ck_customers_document
        CHECK (
            document_type IS NULL
            OR (document_type = 'DNI' AND document_number ~ '^[0-9]{8}$')
            OR (document_type = 'CE' AND document_number ~ '^[0-9]{9,12}$')
            OR (document_type = 'PASSPORT' AND document_number ~ '^[A-Z0-9]{6,12}$')
        ),
    ADD CONSTRAINT ck_customers_password_hash
        CHECK (password_hash IS NULL OR BTRIM(password_hash) <> '');

CREATE UNIQUE INDEX uq_customers_email_lower_per_tenant
    ON customers (tenant_id, LOWER(email))
    WHERE email IS NOT NULL;

CREATE UNIQUE INDEX uq_customers_document_per_tenant
    ON customers (tenant_id, document_type, document_number)
    WHERE document_number IS NOT NULL;

ALTER TABLE customer_addresses
    ADD CONSTRAINT ck_customer_addresses_department_not_blank
        CHECK (BTRIM(department) <> ''),
    ADD CONSTRAINT ck_customer_addresses_province_not_blank
        CHECK (BTRIM(province) <> ''),
    ADD CONSTRAINT ck_customer_addresses_district_not_blank
        CHECK (BTRIM(district) <> ''),
    ADD CONSTRAINT ck_customer_addresses_address_not_blank
        CHECK (BTRIM(address) <> ''),
    ADD CONSTRAINT ck_customer_addresses_coordinates_pair
        CHECK (
            (latitude IS NULL AND longitude IS NULL)
            OR (latitude IS NOT NULL AND longitude IS NOT NULL)
        ),
    ADD CONSTRAINT ck_customer_addresses_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT ck_customer_addresses_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);

CREATE UNIQUE INDEX uq_customer_default_address
    ON customer_addresses (customer_id)
    WHERE is_default = TRUE;

ALTER TABLE carts
    ADD CONSTRAINT uq_carts_tenant_id_id UNIQUE (tenant_id, id),
    ADD CONSTRAINT ck_carts_status
        CHECK (status IN ('ACTIVE', 'ORDERED', 'ABANDONED'));

CREATE UNIQUE INDEX uq_active_cart_per_customer
    ON carts (tenant_id, customer_id)
    WHERE status = 'ACTIVE' AND customer_id IS NOT NULL;

ALTER TABLE product_variants
    ADD CONSTRAINT uq_product_variants_product_id_id UNIQUE (product_id, id);

ALTER TABLE cart_items
    ADD COLUMN tenant_id UUID;

UPDATE cart_items item
SET tenant_id = cart.tenant_id
FROM carts cart
WHERE cart.id = item.cart_id;

ALTER TABLE cart_items
    ALTER COLUMN tenant_id SET NOT NULL,
    DROP CONSTRAINT cart_items_cart_id_fkey,
    DROP CONSTRAINT cart_items_product_id_fkey,
    DROP CONSTRAINT cart_items_variant_id_fkey,
    ADD CONSTRAINT fk_cart_items_cart_same_tenant
        FOREIGN KEY (tenant_id, cart_id)
        REFERENCES carts (tenant_id, id)
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_cart_items_product_same_tenant
        FOREIGN KEY (tenant_id, product_id)
        REFERENCES products (tenant_id, id),
    ADD CONSTRAINT fk_cart_items_variant_product
        FOREIGN KEY (product_id, variant_id)
        REFERENCES product_variants (product_id, id),
    ADD CONSTRAINT ck_cart_items_quantity
        CHECK (quantity > 0),
    ADD CONSTRAINT ck_cart_items_unit_price
        CHECK (unit_price > 0),
    ADD CONSTRAINT uq_cart_items_variant UNIQUE (cart_id, variant_id);

CREATE INDEX idx_customer_addresses_customer_id
    ON customer_addresses (customer_id);

CREATE INDEX idx_carts_tenant_customer_status
    ON carts (tenant_id, customer_id, status);

CREATE INDEX idx_cart_items_tenant_cart
    ON cart_items (tenant_id, cart_id);
