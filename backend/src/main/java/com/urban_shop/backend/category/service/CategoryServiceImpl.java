package com.urban_shop.backend.category.service;

import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.request.CategoryUpdateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.entity.Category;
import com.urban_shop.backend.category.mapper.CategoryMapper;
import com.urban_shop.backend.category.repository.CategoryRepository;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.util.SlugUtils;
import com.urban_shop.backend.tenant.service.PublicTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final PublicTenantResolver publicTenantResolver;

    @Override
    @Transactional
    public CategoryResponse create(UUID tenantId, CategoryCreateRequest request) {
        Category parent = requireOptionalParent(tenantId, request.parentId());
        String name = request.name().trim();
        String slug = SlugUtils.normalizeOrGenerate(request.slug(), name);
        ensureSlugAvailable(tenantId, slug, null);

        Category category = new Category();
        category.setTenantId(tenantId);
        category.setParentId(parent == null ? null : parent.getId());
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(trimToNull(request.description()));
        category.setImageUrl(trimToNull(request.imageUrl()));
        category.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved, parent == null ? null : parent.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listAdmin(UUID tenantId) {
        List<Category> categories = categoryRepository.findAllByTenantIdOrderByDisplayOrderAscNameAsc(tenantId);
        Map<UUID, Category> byId = indexById(categories);
        return categories.stream().map(category -> toResponse(category, byId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listPublic(String tenantSlug) {
        UUID tenantId = publicTenantResolver.requireTenantId(tenantSlug);
        List<Category> categories = categoryRepository.findAllByTenantIdOrderByDisplayOrderAscNameAsc(tenantId);
        Map<UUID, Category> byId = indexById(categories);
        return categories.stream()
            .filter(category -> category.isActive() && hasActiveAncestors(category, byId))
            .map(category -> toResponse(category, byId))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse get(UUID tenantId, UUID categoryId) {
        Category category = requireCategory(tenantId, categoryId);
        Category parent = requireOptionalParent(tenantId, category.getParentId());
        return CategoryMapper.toResponse(category, parent == null ? null : parent.getName());
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID tenantId, UUID categoryId, CategoryUpdateRequest request) {
        Category category = requireCategory(tenantId, categoryId);
        Category parent = requireOptionalParent(tenantId, request.parentId());
        validateHierarchy(tenantId, categoryId, parent);

        String name = request.name().trim();
        String slug = SlugUtils.normalizeOrGenerate(request.slug(), name);
        ensureSlugAvailable(tenantId, slug, categoryId);

        category.setParentId(parent == null ? null : parent.getId());
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(trimToNull(request.description()));
        category.setImageUrl(trimToNull(request.imageUrl()));
        category.setActive(request.active());
        category.setDisplayOrder(request.displayOrder());

        Category saved = categoryRepository.save(category);
        return CategoryMapper.toResponse(saved, parent == null ? null : parent.getName());
    }

    private Category requireCategory(UUID tenantId, UUID categoryId) {
        return categoryRepository.findByTenantIdAndId(tenantId, categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
    }

    private Category requireOptionalParent(UUID tenantId, UUID parentId) {
        return parentId == null ? null : requireCategory(tenantId, parentId);
    }

    private void validateHierarchy(UUID tenantId, UUID categoryId, Category parent) {
        if (parent == null) {
            return;
        }
        if (categoryId.equals(parent.getId())) {
            throw new BusinessException("Una categoria no puede ser su propio padre");
        }

        Set<UUID> visited = new HashSet<>();
        Category current = parent;
        while (current != null && visited.add(current.getId())) {
            if (categoryId.equals(current.getId())) {
                throw new BusinessException("La jerarquia de categorias no puede contener ciclos");
            }
            current = requireOptionalParent(tenantId, current.getParentId());
        }
    }

    private boolean hasActiveAncestors(Category category, Map<UUID, Category> byId) {
        Set<UUID> visited = new HashSet<>();
        UUID parentId = category.getParentId();
        while (parentId != null && visited.add(parentId)) {
            Category parent = byId.get(parentId);
            if (parent == null || !parent.isActive()) {
                return false;
            }
            parentId = parent.getParentId();
        }
        return parentId == null;
    }

    private Map<UUID, Category> indexById(List<Category> categories) {
        Map<UUID, Category> result = new HashMap<>();
        categories.forEach(category -> result.put(category.getId(), category));
        return result;
    }

    private CategoryResponse toResponse(Category category, Map<UUID, Category> byId) {
        Category parent = category.getParentId() == null ? null : byId.get(category.getParentId());
        return CategoryMapper.toResponse(category, parent == null ? null : parent.getName());
    }

    private void ensureSlugAvailable(UUID tenantId, String slug, UUID currentId) {
        boolean exists = currentId == null
            ? categoryRepository.existsByTenantIdAndSlug(tenantId, slug)
            : categoryRepository.existsByTenantIdAndSlugAndIdNot(tenantId, slug, currentId);
        if (exists) {
            throw new BusinessException("Ya existe una categoria con ese slug en la tienda");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
