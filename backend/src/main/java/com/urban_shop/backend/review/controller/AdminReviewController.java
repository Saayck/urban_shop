package com.urban_shop.backend.review.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.review.dto.request.ReviewModerationRequest;
import com.urban_shop.backend.review.dto.response.ReviewResponse;
import com.urban_shop.backend.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/reviews")
@Tag(name = "Admin Reviews", description = "Moderacion de las resenas de la tienda")
@RequiredArgsConstructor
@Validated
public class AdminReviewController {

    private final ReviewService reviewService;
    private final CurrentTenant currentTenant;

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(summary = "Listar todas las resenas, visibles y ocultas")
    public PageResponse<ReviewResponse> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return reviewService.listAdmin(currentTenant.requireId(), page, size);
    }

    @PutMapping("/{id}/moderation")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @Operation(
        summary = "Ocultar o restaurar una resena",
        description = "No borra el contenido: solo deja de mostrarse en la tienda, con constancia del motivo."
    )
    public ReviewResponse moderate(
        @PathVariable UUID id,
        @Valid @RequestBody ReviewModerationRequest request
    ) {
        return reviewService.moderate(currentTenant.requireId(), id, request);
    }
}
