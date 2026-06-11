package com.urban_shop.backend.payment.controller;

import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.payment.dto.request.PaymentCreateRequest;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;
import com.urban_shop.backend.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/orders/{orderId}/payments")
@RequiredArgsConstructor
public class CustomerPaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID orderId,
        @Valid @RequestBody PaymentCreateRequest request
    ) {
        requireCustomer(principal);
        return paymentService.create(
            principal.getTenantId(),
            principal.getPrincipalId(),
            orderId,
            request
        );
    }

    private void requireCustomer(CustomUserDetails principal) {
        if (principal == null || !principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un cliente autenticado");
        }
    }
}
