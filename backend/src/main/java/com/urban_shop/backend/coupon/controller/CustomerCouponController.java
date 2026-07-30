package com.urban_shop.backend.coupon.controller;

import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.coupon.dto.response.CouponPreviewResponse;
import com.urban_shop.backend.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/customer/coupons")
@Tag(name = "Customer Coupons", description = "Validacion de cupones antes de comprar")
@RequiredArgsConstructor
@Validated
public class CustomerCouponController {

    private final CouponService couponService;

    @GetMapping("/preview")
    @Operation(
        summary = "Previsualizar el descuento de un cupon",
        description = "No consume el cupon. Si no aplica devuelve valid=false y el motivo en reason, "
            + "de modo que el frontend pueda mostrarlo sin tratar el caso como un error."
    )
    public CouponPreviewResponse preview(
        @AuthenticationPrincipal CustomUserDetails principal,
        @RequestParam @NotBlank String code,
        @RequestParam BigDecimal subtotal
    ) {
        requireCustomer(principal);
        return couponService.preview(
            principal.getTenantId(),
            principal.getPrincipalId(),
            code,
            subtotal
        );
    }

    private void requireCustomer(CustomUserDetails principal) {
        if (principal == null || !principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un cliente autenticado");
        }
    }
}
