CREATE TABLE shipping_zones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,

    name VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    province VARCHAR(100),
    district VARCHAR(100),

    price DECIMAL(10,2) NOT NULL DEFAULT 0,
    estimated_time VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
