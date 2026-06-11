CREATE TABLE complaints_book (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customers(id),

    complaint_number VARCHAR(50) NOT NULL,

    customer_full_name VARCHAR(150) NOT NULL,
    customer_document_type VARCHAR(30),
    customer_document_number VARCHAR(20),
    customer_email VARCHAR(150),
    customer_phone VARCHAR(20),

    complaint_type VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    requested_solution TEXT,

    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_complaint_number_per_tenant UNIQUE (tenant_id, complaint_number)
);
