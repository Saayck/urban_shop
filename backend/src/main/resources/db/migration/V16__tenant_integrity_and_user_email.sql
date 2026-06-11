ALTER TABLE tenant_business_info
    ADD CONSTRAINT uq_tenant_business_info_tenant UNIQUE (tenant_id);

ALTER TABLE tenant_settings
    ADD CONSTRAINT uq_tenant_settings_tenant UNIQUE (tenant_id);

ALTER TABLE brands
    ADD CONSTRAINT uq_brands_tenant_id_id UNIQUE (tenant_id, id);

ALTER TABLE categories
    ADD CONSTRAINT uq_categories_tenant_id_id UNIQUE (tenant_id, id);

ALTER TABLE products
    ADD CONSTRAINT uq_products_tenant_id_id UNIQUE (tenant_id, id);

ALTER TABLE customers
    ADD CONSTRAINT uq_customers_tenant_id_id UNIQUE (tenant_id, id);

ALTER TABLE orders
    ADD CONSTRAINT uq_orders_tenant_id_id UNIQUE (tenant_id, id);

ALTER TABLE categories
    DROP CONSTRAINT categories_parent_id_fkey,
    ADD CONSTRAINT fk_categories_parent_same_tenant
        FOREIGN KEY (tenant_id, parent_id)
        REFERENCES categories (tenant_id, id);

ALTER TABLE products
    DROP CONSTRAINT products_brand_id_fkey,
    DROP CONSTRAINT products_category_id_fkey,
    ADD CONSTRAINT fk_products_brand_same_tenant
        FOREIGN KEY (tenant_id, brand_id)
        REFERENCES brands (tenant_id, id),
    ADD CONSTRAINT fk_products_category_same_tenant
        FOREIGN KEY (tenant_id, category_id)
        REFERENCES categories (tenant_id, id);

ALTER TABLE carts
    DROP CONSTRAINT carts_customer_id_fkey,
    ADD CONSTRAINT fk_carts_customer_same_tenant
        FOREIGN KEY (tenant_id, customer_id)
        REFERENCES customers (tenant_id, id)
        ON DELETE CASCADE;

ALTER TABLE orders
    DROP CONSTRAINT orders_customer_id_fkey,
    ADD CONSTRAINT fk_orders_customer_same_tenant
        FOREIGN KEY (tenant_id, customer_id)
        REFERENCES customers (tenant_id, id);

ALTER TABLE payments
    DROP CONSTRAINT payments_order_id_fkey,
    ADD CONSTRAINT fk_payments_order_same_tenant
        FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders (tenant_id, id)
        ON DELETE CASCADE;

ALTER TABLE users
    DROP CONSTRAINT users_email_key;

CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));
