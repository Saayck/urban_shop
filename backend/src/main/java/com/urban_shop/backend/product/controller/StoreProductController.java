package com.urban_shop.backend.product.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductResponse;
import com.urban_shop.backend.product.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@SecurityRequirements
@RequestMapping("/api/store/{slug}/products")
@RequiredArgsConstructor
@Validated
public class StoreProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductResponse> list(
        @PathVariable String slug,
        @RequestParam(required = false) UUID brandId,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return productService.listPublic(slug, brandId, categoryId, page, size);
    }

    @GetMapping("/{productSlug}")
    public ProductDetailResponse get(
        @PathVariable String slug,
        @PathVariable String productSlug
    ) {
        return productService.getPublic(slug, productSlug);
    }
}
