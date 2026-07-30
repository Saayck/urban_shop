package com.urban_shop.backend.catalog;

import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.brand.dto.request.BrandCreateRequest;
import com.urban_shop.backend.brand.dto.response.BrandResponse;
import com.urban_shop.backend.brand.service.BrandService;
import com.urban_shop.backend.cart.dto.request.CartItemCreateRequest;
import com.urban_shop.backend.cart.service.CartService;
import com.urban_shop.backend.category.dto.request.CategoryCreateRequest;
import com.urban_shop.backend.category.dto.response.CategoryResponse;
import com.urban_shop.backend.category.service.CategoryService;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.customer.dto.request.CustomerAddressRequest;
import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.service.CustomerService;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import com.urban_shop.backend.order.service.OrderService;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CatalogDeletionIntegrationTest {

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void deletesAnUnusedBrandButNotOneWithProducts() {
        UUID tenantId = createTenant("delete-brand").getId();
        BrandResponse unused = brandService.create(tenantId, new BrandCreateRequest("Marca Libre", null, null));
        BrandResponse inUse = brandService.create(tenantId, new BrandCreateRequest("Marca Usada", null, null));
        CategoryResponse category = categoryService.create(
            tenantId,
            new CategoryCreateRequest(null, "Polos", null, null, null, 0)
        );
        productService.create(tenantId, productRequest(inUse.id(), category.id(), "con-marca"));

        brandService.delete(tenantId, unused.id());
        assertThat(brandService.listAdmin(tenantId)).extracting(BrandResponse::id).containsExactly(inUse.id());

        assertThatThrownBy(() -> brandService.delete(tenantId, inUse.id()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("productos asociados");
    }

    @Test
    void refusesToDeleteACategoryWithChildrenOrProducts() {
        UUID tenantId = createTenant("delete-category").getId();
        CategoryResponse parent = categoryService.create(
            tenantId,
            new CategoryCreateRequest(null, "Ropa", null, null, null, 0)
        );
        CategoryResponse child = categoryService.create(
            tenantId,
            new CategoryCreateRequest(parent.id(), "Polos", null, null, null, 0)
        );

        assertThatThrownBy(() -> categoryService.delete(tenantId, parent.id()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("subcategorias");

        productService.create(tenantId, productRequest(null, child.id(), "con-categoria"));
        assertThatThrownBy(() -> categoryService.delete(tenantId, child.id()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("productos asociados");
    }

    @Test
    void deletesAProductWithItsVariantsWhenItHasNoOrders() {
        UUID tenantId = createTenant("delete-product").getId();
        CategoryResponse category = categoryService.create(
            tenantId,
            new CategoryCreateRequest(null, "Polos", null, null, null, 0)
        );
        ProductDetailResponse product = productService.create(tenantId, productRequest(null, category.id(), "libre"));
        productService.addVariant(
            tenantId,
            product.id(),
            new ProductVariantCreateRequest("SKU-DEL-1", "M", "Negro", "#000000", 5, null, true)
        );

        productService.delete(tenantId, product.id());

        assertThatThrownBy(() -> productService.getAdmin(tenantId, product.id()))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void refusesToDeleteAProductOrVariantAlreadyPresentInOrders() {
        String slug = "delete-ordered";
        Tenant tenant = createTenant(slug);
        UUID tenantId = tenant.getId();
        CategoryResponse category = categoryService.create(
            tenantId,
            new CategoryCreateRequest(null, "Polos", null, null, null, 0)
        );
        ProductDetailResponse product = productService.create(tenantId, productRequest(null, category.id(), "vendido"));
        ProductVariantResponse variant = productService.addVariant(
            tenantId,
            product.id(),
            new ProductVariantCreateRequest("SKU-DEL-2", "M", "Negro", "#000000", 5, null, true)
        );
        productService.updateStatus(tenantId, product.id(), ProductStatus.ACTIVE);

        LoginResponse customer = authService.registerCustomer(new RegisterCustomerRequest(
            slug, "Cliente", "Prueba", slug + "@urban.pe", "987654321", "Password123!", null, null
        ));
        CustomerAddressResponse address = customerService.createAddress(
            tenantId,
            customer.user().id(),
            new CustomerAddressRequest("Lima", "Lima", "Miraflores", "Av. Principal 123", null, null, null, true)
        );
        cartService.addItem(tenantId, customer.user().id(), new CartItemCreateRequest(variant.id(), 1));
        orderService.create(
            tenantId,
            customer.user().id(),
            new OrderCreateRequest(DeliveryType.DELIVERY, address.id(), InvoiceType.BOLETA, "DNI", "12345678", null)
        );

        assertThatThrownBy(() -> productService.delete(tenantId, product.id()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("pedidos asociados");
        assertThatThrownBy(() -> productService.deleteVariant(tenantId, product.id(), variant.id()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("pedidos asociados");
    }

    private ProductCreateRequest productRequest(UUID brandId, UUID categoryId, String slug) {
        return new ProductCreateRequest(
            brandId,
            categoryId,
            "Polo " + slug,
            "polo-" + slug,
            null,
            "Algodon",
            "OVERSIZE",
            new BigDecimal("89.90"),
            null,
            false,
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
