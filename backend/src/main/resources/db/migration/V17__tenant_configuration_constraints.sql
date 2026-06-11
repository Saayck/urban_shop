ALTER TABLE tenants
    ADD CONSTRAINT ck_tenants_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TRIAL', 'CANCELLED')),
    ADD CONSTRAINT ck_tenants_slug_format
        CHECK (slug ~ '^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$'),
    ADD CONSTRAINT ck_tenants_plan_name_format
        CHECK (plan_name ~ '^[A-Z][A-Z0-9_]*$');

ALTER TABLE tenant_business_info
    ADD CONSTRAINT ck_tenant_business_type
        CHECK (business_type IN ('EMPRESA', 'PERSONA_NATURAL')),
    ADD CONSTRAINT ck_tenant_business_ruc_format
        CHECK (ruc IS NULL OR ruc ~ '^[0-9]{11}$'),
    ADD CONSTRAINT ck_tenant_business_company_ruc
        CHECK (business_type <> 'EMPRESA' OR ruc IS NOT NULL),
    ADD CONSTRAINT ck_tenant_business_document_status
        CHECK (document_status IN ('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED')),
    ADD CONSTRAINT ck_tenant_business_phone
        CHECK (phone IS NULL OR phone ~ '^9[0-9]{8}$');

ALTER TABLE tenant_settings
    ADD CONSTRAINT ck_tenant_settings_primary_color
        CHECK (primary_color IS NULL OR primary_color ~ '^#[A-F0-9]{6}$'),
    ADD CONSTRAINT ck_tenant_settings_secondary_color
        CHECK (secondary_color IS NULL OR secondary_color ~ '^#[A-F0-9]{6}$'),
    ADD CONSTRAINT ck_tenant_settings_accent_color
        CHECK (accent_color IS NULL OR accent_color ~ '^#[A-F0-9]{6}$'),
    ADD CONSTRAINT ck_tenant_settings_whatsapp
        CHECK (whatsapp_number IS NULL OR whatsapp_number ~ '^9[0-9]{8}$');
