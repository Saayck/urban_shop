package com.urban_shop.backend.payment.controller;

import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.payment.dto.request.PaymentRejectRequest;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;
import com.urban_shop.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final CurrentTenant currentTenant;

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
