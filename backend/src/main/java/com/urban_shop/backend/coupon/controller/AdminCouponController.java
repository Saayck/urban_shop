package com.urban_shop.backend.coupon.controller;

import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.coupon.dto.request.CouponCreateRequest;
import com.urban_shop.backend.coupon.dto.request.CouponUpdateRequest;
import com.urban_shop.backend.coupon.dto.response.CouponResponse;
import com.urban_shop.backend.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/coupons")
@Tag(name = "Admin Coupons", description = "Cupones de descuento de la tienda")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;
    private final CurrentTenant currentTenant;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(
        summary = "Crear un cupon",
        description = "discountType PERCENTAGE usa discountValue como porcentaje (1-100) y admite "
            + "maxDiscountAmount como tope; FIXED_AMOUNT lo usa como monto fijo en soles."
    )
    public CouponResponse create(@Valid @RequestBody CouponCreateRequest request) {
        return couponService.create(currentTenant.requireId(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(summary = "Listar los cupones de la tienda")
    public List<CouponResponse> list() {
        return couponService.list(currentTenant.requireId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(summary = "Obtener un cupon")
    public CouponResponse get(@PathVariable UUID id) {
        return couponService.get(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(summary = "Actualizar un cupon", description = "El codigo no se puede cambiar una vez creado.")
    public CouponResponse update(@PathVariable UUID id, @Valid @RequestBody CouponUpdateRequest request) {
        return couponService.update(currentTenant.requireId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(
        summary = "Eliminar un cupon",
        description = "Solo si nunca fue canjeado; en caso contrario hay que desactivarlo."
    )
    public void delete(@PathVariable UUID id) {
        couponService.delete(currentTenant.requireId(), id);
    }
}
