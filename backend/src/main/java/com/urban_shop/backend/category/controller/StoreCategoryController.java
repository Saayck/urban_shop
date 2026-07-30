package com.urban_shop.backend.category.controller;

import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirements
@RequestMapping("/api/store/{slug}/categories")
@RequiredArgsConstructor
public class StoreCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> list(@PathVariable String slug) {
        return categoryService.listPublic(slug);
    }
}
