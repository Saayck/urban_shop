package com.urban_shop.backend.brand.controller;

import com.urban_shop.backend.brand.dto.response.BrandResponse;
import com.urban_shop.backend.brand.service.BrandService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirements
@RequestMapping("/api/store/{slug}/brands")
@RequiredArgsConstructor
public class StoreBrandController {

    private final BrandService brandService;

    @GetMapping
    public List<BrandResponse> list(@PathVariable String slug) {
        return brandService.listPublic(slug);
    }
}
