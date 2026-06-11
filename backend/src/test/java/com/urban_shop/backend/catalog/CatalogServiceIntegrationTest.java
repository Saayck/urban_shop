package com.urban_shop.backend.catalog;

import com.urban_shop.backend.brand.dto.request.BrandCreateRequest;
import com.urban_shop.backend.brand.service.BrandService;
import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.request.CategoryUpdateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogServiceIntegrationTest {

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void scopesBrandNamesPerTenantAndRejectsCaseInsensitiveDuplicates() {
        UUID firstTenant = createTenant("catalog-one").getId();
        UUID secondTenant = createTenant("catalog-two").getId();

        brandService.create(firstTenant, new BrandCreateRequest("Urban King", null, null));
        brandService.create(secondTenant, new BrandCreateRequest("Urban King", null, null));

        assertThatThrownBy(() -> brandService.create(
            firstTenant,
            new BrandCreateRequest("urban king", null, null)
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Ya existe una marca");

        assertThat(brandService.listAdmin(firstTenant)).hasSize(1);
        assertThat(brandService.listAdmin(secondTenant)).hasSize(1);
    }

    @Test
    void rejectsCrossTenantParentsAndCategoryCycles() {
        UUID firstTenant = createTenant("category-one").getId();
        UUID secondTenant = createTenant("category-two").getId();
        CategoryResponse foreignParent = categoryService.create(
            secondTenant,
            new CategoryCreateRequest(null, "Externa", null, null, null, 0)
        );

        assertThatThrownBy(() -> categoryService.create(
            firstTenant,
            new CategoryCreateRequest(foreignParent.id(), "Invalida", null, null, null, 0)
        )).isInstanceOf(ResourceNotFoundException.class);

        CategoryResponse root = categoryService.create(
            firstTenant,
            new CategoryCreateRequest(null, "Polos", null, null, null, 0)
        );
        CategoryResponse child = categoryService.create(
            firstTenant,
            new CategoryCreateRequest(root.id(), "Oversize", null, null, null, 1)
        );

        assertThatThrownBy(() -> categoryService.update(
            firstTenant,
            root.id(),
            new CategoryUpdateRequest(
                child.id(),
                "Polos",
                "polos",
                null,
                null,
                true,
                0
            )
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("ciclos");
    }

    private Tenant createTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName(slug);
        tenant.setSlug(slug);
        tenant.setStatus("ACTIVE");
        tenant.setPlanName("BASIC");
        return tenantRepository.save(tenant);
    }
}
