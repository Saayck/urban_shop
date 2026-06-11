CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    plan_name VARCHAR(50) NOT NULL DEFAULT 'BASIC',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE tenant_business_info (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,

    business_type VARCHAR(50) NOT NULL,
    commercial_name VARCHAR(150) NOT NULL,
    legal_name VARCHAR(200),
    ruc VARCHAR(11),
    document_status VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION',

    phone VARCHAR(20),
    email VARCHAR(150),
    address TEXT,
    district VARCHAR(100),
    province VARCHAR(100),
    department VARCHAR(100),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tenant_business_ruc UNIQUE (tenant_id, ruc)
);
