package com.urban_shop.backend.review.controller;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.review.dto.response.ReviewResponse;
import com.urban_shop.backend.review.dto.response.ReviewSummary;
import com.urban_shop.backend.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirements
@RequestMapping("/api/store/{slug}/products/{productSlug}/reviews")
@Tag(name = "Store Reviews", description = "Resenas publicas de un producto")
@RequiredArgsConstructor
@Validated
public class StoreReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Listar las resenas visibles de un producto")
    public PageResponse<ReviewResponse> list(
        @PathVariable String slug,
        @PathVariable String productSlug,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return reviewService.listPublic(slug, productSlug, page, size);
    }

    @GetMapping("/summary")
    @Operation(summary = "Promedio y total de valoraciones de un producto")
    public ReviewSummary summary(@PathVariable String slug, @PathVariable String productSlug) {
        return reviewService.summaryPublic(slug, productSlug);
    }
}
