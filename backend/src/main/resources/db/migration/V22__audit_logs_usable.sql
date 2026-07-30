-- La tabla audit_logs existia desde V13 sin ningun modulo Java que la usara.
-- Guardamos descripciones de texto plano, no documentos JSON, asi que las columnas
-- pasan a TEXT para que la entidad JPA valide contra el esquema real.
ALTER TABLE audit_logs
    ALTER COLUMN old_value TYPE TEXT USING old_value::TEXT,
    ALTER COLUMN new_value TYPE TEXT USING new_value::TEXT,
    ADD CONSTRAINT ck_audit_logs_action_not_blank CHECK (BTRIM(action) <> '');

CREATE INDEX idx_audit_logs_tenant_created_at
    ON audit_logs (tenant_id, created_at DESC);

CREATE INDEX idx_audit_logs_tenant_action
    ON audit_logs (tenant_id, action);

CREATE INDEX idx_audit_logs_tenant_entity
    ON audit_logs (tenant_id, entity_name, entity_id);
