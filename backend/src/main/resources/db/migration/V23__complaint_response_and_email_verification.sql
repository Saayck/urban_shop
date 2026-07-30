-- El Libro de Reclamaciones obliga a dejar constancia escrita de la respuesta al
-- consumidor. Hasta ahora solo se guardaba el estado, sin el texto de la respuesta.
ALTER TABLE complaints_book
    ADD COLUMN response_text TEXT,
    ADD COLUMN responded_at TIMESTAMP,
    ADD CONSTRAINT ck_complaints_answered_has_response
        CHECK (
            status <> 'ANSWERED'
            OR (
                response_text IS NOT NULL
                AND BTRIM(response_text) <> ''
                AND responded_at IS NOT NULL
            )
        );

CREATE INDEX idx_complaints_tenant_status
    ON complaints_book (tenant_id, status);

-- Verificacion de correo. Las columnas email_verified existian en users y customers
-- pero nada las ponia nunca en true.
CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,

    email VARCHAR(150) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,

    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_email_verification_principal_type
        CHECK (principal_type IN ('USER', 'CUSTOMER')),
    CONSTRAINT ck_email_verification_email_not_blank
        CHECK (BTRIM(email) <> ''),
    CONSTRAINT ck_email_verification_customer_tenant
        CHECK (principal_type <> 'CUSTOMER' OR tenant_id IS NOT NULL)
);

CREATE INDEX idx_email_verification_tokens_email
    ON email_verification_tokens (LOWER(email));

CREATE INDEX idx_email_verification_tokens_expires_at
    ON email_verification_tokens (expires_at);

-- Indice para la busqueda de clientes del panel de administracion.
CREATE INDEX idx_customers_tenant_status
    ON customers (tenant_id, status);
