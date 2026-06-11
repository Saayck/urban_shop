package com.urban_shop.backend.customer.controller;

import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.customer.dto.request.CustomerAddressRequest;
import com.urban_shop.backend.customer.dto.request.CustomerUpdateRequest;
import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.dto.response.CustomerResponse;
import com.urban_shop.backend.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public CustomerResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        requireCustomer(principal);
        return customerService.get(principal.getTenantId(), principal.getPrincipalId());
    }

    @PutMapping("/me")
    public CustomerResponse update(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody CustomerUpdateRequest request
    ) {
        requireCustomer(principal);
        return customerService.update(principal.getTenantId(), principal.getPrincipalId(), request);
    }

    @GetMapping("/addresses")
    public List<CustomerAddressResponse> listAddresses(
        @AuthenticationPrincipal CustomUserDetails principal
    ) {
        requireCustomer(principal);
        return customerService.listAddresses(principal.getTenantId(), principal.getPrincipalId());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAddressResponse createAddress(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody CustomerAddressRequest request
    ) {
        requireCustomer(principal);
        return customerService.createAddress(
            principal.getTenantId(),
            principal.getPrincipalId(),
            request
        );
    }

    @PutMapping("/addresses/{id}")
    public CustomerAddressResponse updateAddress(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id,
        @Valid @RequestBody CustomerAddressRequest request
    ) {
        requireCustomer(principal);
        return customerService.updateAddress(
            principal.getTenantId(),
            principal.getPrincipalId(),
            id,
            request
        );
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id
    ) {
        requireCustomer(principal);
        customerService.deleteAddress(principal.getTenantId(), principal.getPrincipalId(), id);
    }

    private void requireCustomer(CustomUserDetails principal) {
        if (principal == null || !principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un cliente autenticado");
        }
    }
}
