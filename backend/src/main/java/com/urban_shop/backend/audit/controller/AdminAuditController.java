package com.urban_shop.backend.audit.controller;

import com.urban_shop.backend.audit.dto.response.AuditLogResponse;
import com.urban_shop.backend.audit.service.AuditService;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/audit-logs")
@Tag(name = "Admin Audit", description = "Bitacora de acciones sensibles de la tienda")
@RequiredArgsConstructor
@Validated
public class AdminAuditController {

    private final AuditService auditService;
    private final CurrentTenant currentTenant;

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(
        summary = "Consultar la bitacora de auditoria",
        description = "Filtra por accion, entidad y rango de fechas. Ordenado del evento mas reciente al mas antiguo."
    )
    public PageResponse<AuditLogResponse> search(
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String entityName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return auditService.search(currentTenant.requireId(), action, entityName, from, to, page, size);
    }
}
