package com.urban_shop.backend.user.controller;

import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.user.dto.request.StaffUserCreateRequest;
import com.urban_shop.backend.user.dto.request.StaffUserUpdateRequest;
import com.urban_shop.backend.user.dto.response.StaffUserResponse;
import com.urban_shop.backend.user.service.StaffUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin Staff", description = "Gestion del personal interno de la tienda")
@RequiredArgsConstructor
public class AdminStaffUserController {

    private final StaffUserService staffUserService;
    private final CurrentTenant currentTenant;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Crear un usuario interno (TENANT_ADMIN o SALES_STAFF)")
    public StaffUserResponse create(@Valid @RequestBody StaffUserCreateRequest request) {
        return staffUserService.create(currentTenant.requireId(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Listar el personal de la tienda")
    public List<StaffUserResponse> list() {
        return staffUserService.list(currentTenant.requireId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Obtener un usuario interno")
    public StaffUserResponse get(@PathVariable UUID id) {
        return staffUserService.get(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(
        summary = "Actualizar datos, rol y estado de un usuario interno",
        description = "Al desactivar un usuario se revocan todas sus sesiones abiertas."
    )
    public StaffUserResponse update(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id,
        @Valid @RequestBody StaffUserUpdateRequest request
    ) {
        return staffUserService.update(currentTenant.requireId(), principal.getPrincipalId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(
        summary = "Desactivar un usuario interno",
        description = "No borra el registro: lo desactiva y revoca sus sesiones, conservando la trazabilidad."
    )
    public void deactivate(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id
    ) {
        staffUserService.deactivate(currentTenant.requireId(), principal.getPrincipalId(), id);
    }
}
