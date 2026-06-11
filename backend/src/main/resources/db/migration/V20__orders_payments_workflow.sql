UPDATE orders
SET invoice_type = 'BOLETA'
WHERE invoice_type IS NULL;

ALTER TABLE orders
    ALTER COLUMN invoice_type SET NOT NULL,
    ADD CONSTRAINT ck_orders_subtotal
        CHECK (subtotal >= 0),
    ADD CONSTRAINT ck_orders_shipping_cost
        CHECK (shipping_cost >= 0),
    ADD CONSTRAINT ck_orders_discount_total
        CHECK (discount_total >= 0),
    ADD CONSTRAINT ck_orders_total
        CHECK (
            total >= 0
            AND discount_total <= subtotal + shipping_cost
            AND total = subtotal + shipping_cost - discount_total
        ),
    ADD CONSTRAINT ck_orders_order_number_not_blank
        CHECK (BTRIM(order_number) <> ''),
    ADD CONSTRAINT ck_orders_payment_status
        CHECK (payment_status IN (
            'PENDING', 'PAID', 'FAILED', 'CANCELLED', 'REFUNDED', 'MANUAL_REVIEW'
        )),
    ADD CONSTRAINT ck_orders_order_status
        CHECK (order_status IN (
            'CREATED', 'PAID', 'PREPARING', 'READY_FOR_PICKUP',
            'ON_THE_WAY', 'DELIVERED', 'CANCELLED', 'REFUNDED'
        )),
    ADD CONSTRAINT ck_orders_delivery_type
        CHECK (delivery_type IN ('DELIVERY', 'STORE_PICKUP')),
    ADD CONSTRAINT ck_orders_delivery_address
        CHECK (
            delivery_type <> 'DELIVERY'
            OR (
                department IS NOT NULL
                AND province IS NOT NULL
                AND district IS NOT NULL
                AND address IS NOT NULL
                AND BTRIM(department) <> ''
                AND BTRIM(province) <> ''
                AND BTRIM(district) <> ''
                AND BTRIM(address) <> ''
            )
        ),
    ADD CONSTRAINT ck_orders_coordinates_pair
        CHECK (
            (latitude IS NULL AND longitude IS NULL)
            OR (latitude IS NOT NULL AND longitude IS NOT NULL)
        ),
    ADD CONSTRAINT ck_orders_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT ck_orders_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    ADD CONSTRAINT ck_orders_invoice_type
        CHECK (invoice_type IN ('BOLETA', 'FACTURA')),
    ADD CONSTRAINT ck_orders_document_pair
        CHECK (
            (document_type IS NULL AND document_number IS NULL)
            OR (document_type IS NOT NULL AND document_number IS NOT NULL)
        ),
    ADD CONSTRAINT ck_orders_document
        CHECK (
            document_type IS NULL
            OR (invoice_type = 'FACTURA' AND document_type = 'RUC'
                AND document_number ~ '^[0-9]{11}$')
            OR (invoice_type = 'BOLETA' AND document_type = 'DNI'
                AND document_number ~ '^[0-9]{8}$')
            OR (invoice_type = 'BOLETA' AND document_type = 'CE'
                AND document_number ~ '^[0-9]{9,12}$')
            OR (invoice_type = 'BOLETA' AND document_type = 'PASSPORT'
                AND document_number ~ '^[A-Z0-9]{6,12}$')
        ),
    ADD CONSTRAINT ck_orders_factura_ruc
        CHECK (
            invoice_type <> 'FACTURA'
            OR (
                document_type = 'RUC'
                AND document_number IS NOT NULL
                AND document_number ~ '^[0-9]{11}$'
            )
        ),
    ADD CONSTRAINT ck_orders_paid_workflow
        CHECK (
            order_status NOT IN (
                'PAID', 'PREPARING', 'READY_FOR_PICKUP', 'ON_THE_WAY', 'DELIVERED'
            )
            OR payment_status = 'PAID'
        );

ALTER TABLE order_items
    ADD COLUMN tenant_id UUID;

UPDATE order_items item
SET tenant_id = customer_order.tenant_id
FROM orders customer_order
WHERE customer_order.id = item.order_id;

ALTER TABLE order_items
    ALTER COLUMN tenant_id SET NOT NULL,
    DROP CONSTRAINT order_items_order_id_fkey,
    DROP CONSTRAINT order_items_product_id_fkey,
    DROP CONSTRAINT order_items_variant_id_fkey,
    ADD CONSTRAINT fk_order_items_order_same_tenant
        FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders (tenant_id, id)
        ON DELETE CASCADE,
    ADD CONSTRAINT fk_order_items_product_same_tenant
        FOREIGN KEY (tenant_id, product_id)
        REFERENCES products (tenant_id, id),
    ADD CONSTRAINT fk_order_items_variant_product
        FOREIGN KEY (product_id, variant_id)
        REFERENCES product_variants (product_id, id),
    ADD CONSTRAINT ck_order_items_product_name_not_blank
        CHECK (BTRIM(product_name) <> ''),
    ADD CONSTRAINT ck_order_items_size_not_blank
        CHECK (BTRIM(size) <> ''),
    ADD CONSTRAINT ck_order_items_color_not_blank
        CHECK (BTRIM(color) <> ''),
    ADD CONSTRAINT ck_order_items_quantity
        CHECK (quantity > 0),
    ADD CONSTRAINT ck_order_items_unit_price
        CHECK (unit_price > 0),
    ADD CONSTRAINT ck_order_items_total_price
        CHECK (total_price = unit_price * quantity),
    ADD CONSTRAINT uq_order_items_variant UNIQUE (order_id, variant_id);

ALTER TABLE payments
    ADD COLUMN reviewed_by UUID REFERENCES users(id),
    ADD COLUMN reviewed_at TIMESTAMP,
    ADD COLUMN rejection_reason VARCHAR(500),
    ADD CONSTRAINT ck_payments_provider_not_blank
        CHECK (BTRIM(provider) <> ''),
    ADD CONSTRAINT ck_payments_method
        CHECK (method IN ('YAPE', 'PLIN', 'BANK_TRANSFER', 'CASH_ON_DELIVERY')),
    ADD CONSTRAINT ck_payments_amount
        CHECK (amount > 0),
    ADD CONSTRAINT ck_payments_currency
        CHECK (currency = 'PEN'),
    ADD CONSTRAINT ck_payments_status
        CHECK (status IN (
            'PENDING', 'PAID', 'FAILED', 'CANCELLED', 'REFUNDED', 'MANUAL_REVIEW'
        )),
    ADD CONSTRAINT ck_payments_manual_evidence
        CHECK (
            method = 'CASH_ON_DELIVERY'
            OR (
                operation_code IS NOT NULL
                AND BTRIM(operation_code) <> ''
            )
            OR (
                proof_image_url IS NOT NULL
                AND BTRIM(proof_image_url) <> ''
            )
        ),
    ADD CONSTRAINT ck_payments_cash_evidence
        CHECK (
            method <> 'CASH_ON_DELIVERY'
            OR (operation_code IS NULL AND proof_image_url IS NULL)
        ),
    ADD CONSTRAINT ck_payments_paid_at
        CHECK (status <> 'PAID' OR paid_at IS NOT NULL),
    ADD CONSTRAINT ck_payments_review
        CHECK (
            status NOT IN ('PAID', 'FAILED')
            OR (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
        ),
    ADD CONSTRAINT ck_payments_rejection_reason
        CHECK (
            status <> 'FAILED'
            OR (
                rejection_reason IS NOT NULL
                AND BTRIM(rejection_reason) <> ''
            )
        );

CREATE UNIQUE INDEX uq_open_payment_per_order
    ON payments (tenant_id, order_id)
    WHERE status IN ('PENDING', 'MANUAL_REVIEW');

CREATE UNIQUE INDEX uq_payment_operation_per_tenant
    ON payments (tenant_id, provider, LOWER(operation_code))
    WHERE operation_code IS NOT NULL;

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    changed_by UUID REFERENCES users(id),
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_order_status_history_order_same_tenant
        FOREIGN KEY (tenant_id, order_id)
        REFERENCES orders (tenant_id, id)
        ON DELETE CASCADE,
    CONSTRAINT ck_order_status_history_status
        CHECK (status IN (
            'CREATED', 'PAID', 'PREPARING', 'READY_FOR_PICKUP',
            'ON_THE_WAY', 'DELIVERED', 'CANCELLED', 'REFUNDED'
        ))
);

INSERT INTO order_status_history (tenant_id, order_id, status, notes, created_at)
SELECT tenant_id, id, order_status, 'Estado inicial migrado', created_at
FROM orders;

CREATE INDEX idx_order_items_tenant_order
    ON order_items (tenant_id, order_id);

CREATE INDEX idx_orders_tenant_created_at
    ON orders (tenant_id, created_at DESC);

CREATE INDEX idx_payments_tenant_status
    ON payments (tenant_id, status);

CREATE INDEX idx_order_status_history_order_created_at
    ON order_status_history (order_id, created_at);
