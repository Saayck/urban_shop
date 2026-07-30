-- Tokens de restablecimiento de contrasena persistidos.
-- Antes vivian en un mapa estatico en memoria: se perdian al reiniciar, no funcionaban
-- con mas de una instancia y nunca se purgaban.
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,

    email VARCHAR(150) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,

    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_password_reset_tokens_principal_type
        CHECK (principal_type IN ('USER', 'CUSTOMER')),
    CONSTRAINT ck_password_reset_tokens_email_not_blank
        CHECK (BTRIM(email) <> ''),
    CONSTRAINT ck_password_reset_tokens_customer_tenant
        CHECK (principal_type <> 'CUSTOMER' OR tenant_id IS NOT NULL)
);

CREATE INDEX idx_password_reset_tokens_email
    ON password_reset_tokens (LOWER(email));

CREATE INDEX idx_password_reset_tokens_expires_at
    ON password_reset_tokens (expires_at);

-- Refresh tokens opacos y revocables: habilitan renovar el acceso sin re-autenticar
-- y dan semantica real al logout.
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,

    principal_id UUID NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,

    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_refresh_tokens_principal_type
        CHECK (principal_type IN ('USER', 'CUSTOMER'))
);

CREATE INDEX idx_refresh_tokens_principal
    ON refresh_tokens (principal_id)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_refresh_tokens_expires_at
    ON refresh_tokens (expires_at);

-- Los pagos aprobados por webhook no tienen un revisor humano: se identifican por el
-- external_payment_id que devuelve la pasarela.
ALTER TABLE payments
    DROP CONSTRAINT ck_payments_review,
    ADD CONSTRAINT ck_payments_review
        CHECK (
            status NOT IN ('PAID', 'FAILED')
            OR (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
            OR (external_payment_id IS NOT NULL AND reviewed_at IS NOT NULL)
        );
