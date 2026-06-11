package com.urban_shop.backend.product;

import com.urban_shop.backend.brand.dto.request.BrandCreateRequest;
import com.urban_shop.backend.brand.dto.response.BrandResponse;
import com.urban_shop.backend.brand.service.BrandService;
import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.inventory.dto.request.InventoryMovementRequest;
import com.urban_shop.backend.inventory.entity.InventoryMovementType;
import com.urban_shop.backend.inventory.service.InventoryService;
import com.urban_shop.backend.product.dto.request.ProductCreateRequest;
import com.urban_shop.backend.product.dto.request.ProductVariantCreateRequest;
import com.urban_shop.backend.product.dto.response.ProductDetailResponse;
import com.urban_shop.backend.product.dto.response.ProductVariantResponse;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.service.ProductService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductInventoryIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void requiresCategoryAndActiveVariantBeforePublishing() {
        UUID tenantId = createTenant("publish-rules").getId();
        ProductDetailResponse product = productService.create(
            tenantId,
            productRequest(null, null, "Polo sin categoria", "polo-sin-categoria")
        );

        assertThatThrownBy(() -> productService.updateStatus(
            tenantId,
            product.id(),
            ProductStatus.ACTIVE
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("categoria activa");

        CategoryResponse category = categoryService.create(
            tenantId,
            new CategoryCreateRequest(null, "Polos", null, null, null, 0)
        );
        ProductDetailResponse categorized = productService.create(
            tenantId,
            productRequest(null, category.id(), "Polo sin variante", "polo-sin-variante")
        );

        assertThatThrownBy(() -> productService.updateStatus(
            tenantId,
            categorized.id(),
            ProductStatus.ACTIVE
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("variante activa");
    }

    @Test
    void tracksInitialStockAndSynchronizesPublishedProductStatus() {
        UUID tenantId = createTenant("stock-flow").getId();
        BrandResponse brand = brandService.create(
            tenantId,
            new BrandCreateRequest("Urban King", null, null)
        );
        CategoryResponse category = categoryService.create(
            tenantId,
            new CategoryCreateRequest(null, "Polos", null, null, null, 0)
        );
        ProductDetailResponse product = productService.create(
            tenantId,
            productRequest(brand.id(), category.id(), "Polo Oversize", "polo-oversize")
        );
        ProductVariantResponse variant = productService.addVariant(
            tenantId,
            product.id(),
            new ProductVariantCreateRequest(
                "POLO-BLK-M",
                "M",
                "Negro",
                "#000000",
                3,
                null,
                true
            )
        );

        assertThat(productService.updateStatus(tenantId, product.id(), ProductStatus.ACTIVE).status())
            .isEqualTo(ProductStatus.ACTIVE);

        inventoryService.create(
            tenantId,
            new InventoryMovementRequest(variant.id(), InventoryMovementType.EXIT, 3, "Ajuste de prueba")
        );
        assertThat(productService.getAdmin(tenantId, product.id()).status())
            .isEqualTo(ProductStatus.OUT_OF_STOCK);

        inventoryService.create(
            tenantId,
            new InventoryMovementRequest(variant.id(), InventoryMovementType.ENTRY, 2, "Reposicion")
        );
        ProductDetailResponse replenished = productService.getAdmin(tenantId, product.id());
        assertThat(replenished.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(replenished.totalStock()).isEqualTo(2);
        assertThat(inventoryService.list(tenantId, 0, 20).totalElements()).isEqualTo(3);

        assertThatThrownBy(() -> inventoryService.create(
            tenantId,
            new InventoryMovementRequest(variant.id(), InventoryMovementType.EXIT, 3, "Sin stock")
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void rejectsDuplicateVariantAndCrossTenantProductLookup() {
        UUID tenantId = createTenant("variant-owner").getId();
        UUID otherTenantId = createTenant("variant-other").getId();
        CategoryResponse category = categoryService.create(
            tenantId,
            new CategoryCreateRequest(null, "Polos", null, null, null, 0)
        );
        ProductDetailResponse product = productService.create(
            tenantId,
            productRequest(null, category.id(), "Polo Negro", "polo-negro")
        );
        productService.addVariant(
            tenantId,
            product.id(),
            new ProductVariantCreateRequest(null, "m", "Negro", null, 0, null, true)
        );

        assertThatThrownBy(() -> productService.addVariant(
            tenantId,
            product.id(),
            new ProductVariantCreateRequest(null, "M", "negro", null, 0, null, true)
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Ya existe una variante");

        assertThatThrownBy(() -> productService.getAdmin(otherTenantId, product.id()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsSalePriceAboveBasePrice() {
        UUID tenantId = createTenant("price-rules").getId();

        assertThatThrownBy(() -> productService.create(
            tenantId,
            new ProductCreateRequest(
                null,
                null,
                "Precio invalido",
                "precio-invalido",
                null,
                null,
                null,
                new BigDecimal("50.00"),
                new BigDecimal("60.00"),
                false,
                false
            )
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("precio de oferta");
    }

    private ProductCreateRequest productRequest(
        UUID brandId,
        UUID categoryId,
        String name,
        String slug
    ) {
        return new ProductCreateRequest(
            brandId,
            categoryId,
            name,
            slug,
            "Descripcion",
            "Algodon",
            "OVERSIZE",
            new BigDecimal("89.90"),
            new BigDecimal("79.90"),
            true,
            true
        );
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
