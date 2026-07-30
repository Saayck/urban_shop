package com.urban_shop.backend.review.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.review.dto.request.ReviewCreateRequest;
import com.urban_shop.backend.review.dto.request.ReviewUpdateRequest;
import com.urban_shop.backend.review.dto.response.ReviewResponse;
import com.urban_shop.backend.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/reviews")
@Tag(name = "Customer Reviews", description = "Resenas del cliente autenticado")
@RequiredArgsConstructor
@Validated
public class CustomerReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Publicar una resena",
        description = "Solo se admite si el cliente compro el producto en un pedido ya entregado, "
            + "y una unica vez por producto."
    )
    public ReviewResponse create(
        @AuthenticationPrincipal CustomUserDetails principal,
        @Valid @RequestBody ReviewCreateRequest request
    ) {
        requireCustomer(principal);
        return reviewService.create(principal.getTenantId(), principal.getPrincipalId(), request);
    }

    @GetMapping
    @Operation(summary = "Listar mis resenas")
    public PageResponse<ReviewResponse> listMine(
        @AuthenticationPrincipal CustomUserDetails principal,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        requireCustomer(principal);
        return reviewService.listMine(principal.getTenantId(), principal.getPrincipalId(), page, size);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar mi resena")
    public ReviewResponse update(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id,
        @Valid @RequestBody ReviewUpdateRequest request
    ) {
        requireCustomer(principal);
        return reviewService.update(principal.getTenantId(), principal.getPrincipalId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar mi resena")
    public void delete(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id
    ) {
        requireCustomer(principal);
        reviewService.delete(principal.getTenantId(), principal.getPrincipalId(), id);
    }

    private void requireCustomer(CustomUserDetails principal) {
        if (principal == null || !principal.isCustomer()) {
            throw new AccessDeniedException("Se requiere un cliente autenticado");
        }
    }
}
