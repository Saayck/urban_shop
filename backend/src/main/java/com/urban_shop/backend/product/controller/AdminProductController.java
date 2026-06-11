package com.urban_shop.backend.product.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.product.dto.request.ProductCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductImageRequest;
import com.urban_shop.backend.product.dto.request.ProductStatusRequest;
import com.urban_shop.backend.product.dto.request.ProductUpdateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantUpdateRequest;
import com.urban_shop.backend.product.dto.request.SizeGuideRequest;
import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductImageResponse;
import com.urban_shop.backend.product.dto.response.ProductResponse;
import com.urban_shop.backend.product.dto.response.ProductVariantResponse;
import com.urban_shop.backend.product.dto.response.SizeGuideResponse;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Validated
public class AdminProductController {

    private final ProductService productService;
    private final CurrentTenant currentTenant;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ProductDetailResponse create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(currentTenant.requireId(), request);
    }

    @GetMapping
    public PageResponse<ProductResponse> list(
        @RequestParam(required = false) ProductStatus status,
        @RequestParam(required = false) UUID brandId,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return productService.listAdmin(
            currentTenant.requireId(),
            status,
            brandId,
            categoryId,
            page,
            size
        );
    }

    @GetMapping("/{id}")
    public ProductDetailResponse get(@PathVariable UUID id) {
        return productService.getAdmin(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ProductDetailResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody ProductUpdateRequest request
    ) {
        return productService.update(currentTenant.requireId(), id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ProductDetailResponse updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody ProductStatusRequest request
    ) {
        return productService.updateStatus(currentTenant.requireId(), id, request.status());
    }

    @PostMapping("/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ProductImageResponse addImage(
        @PathVariable UUID id,
        @Valid @RequestBody ProductImageRequest request
    ) {
        return productService.addImage(currentTenant.requireId(), id, request);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        productService.deleteImage(currentTenant.requireId(), id, imageId);
    }

    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ProductVariantResponse addVariant(
        @PathVariable UUID id,
        @Valid @RequestBody ProductVariantCreateRequest request
    ) {
        return productService.addVariant(currentTenant.requireId(), id, request);
    }

    @PutMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ProductVariantResponse updateVariant(
        @PathVariable UUID id,
        @PathVariable UUID variantId,
        @Valid @RequestBody ProductVariantUpdateRequest request
    ) {
        return productService.updateVariant(currentTenant.requireId(), id, variantId, request);
    }

    @PostMapping("/{id}/size-guides")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public SizeGuideResponse addSizeGuide(
        @PathVariable UUID id,
        @Valid @RequestBody SizeGuideRequest request
    ) {
        return productService.addSizeGuide(currentTenant.requireId(), id, request);
    }

    @PutMapping("/{id}/size-guides/{guideId}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public SizeGuideResponse updateSizeGuide(
        @PathVariable UUID id,
        @PathVariable UUID guideId,
        @Valid @RequestBody SizeGuideRequest request
    ) {
        return productService.updateSizeGuide(currentTenant.requireId(), id, guideId, request);
    }

    @DeleteMapping("/{id}/size-guides/{guideId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void deleteSizeGuide(@PathVariable UUID id, @PathVariable UUID guideId) {
        productService.deleteSizeGuide(currentTenant.requireId(), id, guideId);
    }
}
