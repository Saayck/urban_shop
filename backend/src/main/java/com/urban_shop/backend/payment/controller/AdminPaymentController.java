package com.urban_shop.backend.payment.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.payment.dto.request.PaymentRejectRequest;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;
import com.urban_shop.backend.payment.entity.PaymentStatus;
import com.urban_shop.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@Validated
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final CurrentTenant currentTenant;

    @GetMapping
    public PageResponse<PaymentResponse> list(
        @RequestParam(required = false) PaymentStatus status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return paymentService.listAdmin(currentTenant.requireId(), status, page, size);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return paymentService.getAdmin(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}/confirm")
    public PaymentResponse confirm(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id
    ) {
        requireInternal(principal);
        return paymentService.confirm(currentTenant.requireId(), principal.getPrincipalId(), id);
    }

    @PutMapping("/{id}/reject")
    public PaymentResponse reject(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id,
        @Valid @RequestBody PaymentRejectRequest request
    ) {
        requireInternal(principal);
        return paymentService.reject(
            currentTenant.requireId(),
            principal.getPrincipalId(),
            id,
            request
        );
    }

    private void requireInternal(CustomUserDetails principal) {
        if (principal == null || principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un usuario interno");
        }
    }
}
