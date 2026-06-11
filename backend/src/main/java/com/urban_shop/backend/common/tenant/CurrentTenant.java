package com.urban_shop.backend.common.tenant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentTenant {

    public UUID requireId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("El usuario autenticado no tiene tenant asignado");
        }
        return tenantId;
    }
}
