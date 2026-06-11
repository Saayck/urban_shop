CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),

    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100),
    entity_id UUID,

    old_value JSONB,
    new_value JSONB,

    ip_address VARCHAR(50),
    user_agent TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
