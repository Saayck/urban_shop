package com.urban_shop.backend.category.controller;

import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.request.CategoryUpdateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import com.urban_shop.backend.common.tenant.CurrentTenant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final CurrentTenant currentTenant;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public CategoryResponse create(@Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.create(currentTenant.requireId(), request);
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.listAdmin(currentTenant.requireId());
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable UUID id) {
        return categoryService.get(currentTenant.requireId(), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.update(currentTenant.requireId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void delete(@PathVariable UUID id) {
        categoryService.delete(currentTenant.requireId(), id);
    }
}
