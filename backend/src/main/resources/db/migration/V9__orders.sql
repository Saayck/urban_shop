CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customers(id),

    order_number VARCHAR(50) NOT NULL,

    subtotal DECIMAL(10,2) NOT NULL,
    shipping_cost DECIMAL(10,2) NOT NULL DEFAULT 0,
    discount_total DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,

    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    order_status VARCHAR(30) NOT NULL DEFAULT 'CREATED',

    delivery_type VARCHAR(30) NOT NULL DEFAULT 'DELIVERY',

    department VARCHAR(100),
    province VARCHAR(100),
    district VARCHAR(100),
    address TEXT,
    reference TEXT,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),

    document_type VARCHAR(30),
    document_number VARCHAR(20),
    invoice_type VARCHAR(30),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_order_number_per_tenant UNIQUE (tenant_id, order_number)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,

    product_id UUID NOT NULL REFERENCES products(id),
    variant_id UUID NOT NULL REFERENCES product_variants(id),

    product_name VARCHAR(150) NOT NULL,
    size VARCHAR(30) NOT NULL,
    color VARCHAR(50) NOT NULL,

    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
