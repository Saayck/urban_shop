package com.urban_shop.backend.audit.service;

import com.urban_shop.backend.audit.dto.response.AuditLogResponse;
import com.urban_shop.backend.common.response.PageResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Registro de acciones sensibles sobre la tabla {@code audit_logs}.
 */
public interface AuditService {

    /**
     * Deja constancia de una accion. Nunca propaga errores: una auditoria fallida
     * no debe abortar la operacion de negocio que la origina.
     */
    void record(String action, String entityName, UUID entityId, String oldValue, String newValue);

    PageResponse<AuditLogResponse> search(
        UUID tenantId,
        String action,
        String entityName,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    );
}
