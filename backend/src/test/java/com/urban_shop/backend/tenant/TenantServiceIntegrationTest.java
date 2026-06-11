package com.urban_shop.backend.tenant;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.template.entity.StoreTemplate;
import com.urban_shop.backend.template.repository.StoreTemplateRepository;
import com.urban_shop.backend.tenant.dto.request.BusinessInfoRequest;
import com.urban_shop.backend.tenant.dto.request.CreateTenantRequest;
import com.urban_shop.backend.tenant.dto.request.InitialAdminRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateBusinessInfoRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateTenantRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateTenantSettingsRequest;
import com.urban_shop.backend.tenant.dto.response.TenantDetailResponse;
import com.urban_shop.backend.tenant.entity.TenantBusinessInfo;
import com.urban_shop.backend.tenant.repository.TenantBusinessInfoRepository;
import com.urban_shop.backend.tenant.service.TenantService;
import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.repository.RoleRepository;
import com.urban_shop.backend.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantServiceIntegrationTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantBusinessInfoRepository businessInfoRepository;

    @Autowired
    private StoreTemplateRepository templateRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    protected void setUp() {
        if (roleRepository.findByName("TENANT_ADMIN").isEmpty()) {
            Role role = new Role();
            role.setName("TENANT_ADMIN");
            role.setDescription("Administrador de una tienda");
            roleRepository.save(role);
        }
    }

    @Test
    void createsTenantWithBusinessInfoSettingsAndAdmin() {
        TenantDetailResponse response = tenantService.create(createRequest("urban-style", "admin@urban.pe"));

        assertThat(response.name()).isEqualTo("Urban Style");
        assertThat(response.slug()).isEqualTo("urban-style");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.businessInfo().businessType()).isEqualTo("EMPRESA");
        assertThat(response.businessInfo().email()).isEqualTo("ventas@urban.pe");
        assertThat(response.settings()).isNotNull();
        assertThat(tenantService.getPublicBySlug("urban-style").businessInfo().commercialName())
            .isEqualTo("Urban Style");
        assertThat(userRepository.findByEmailIgnoreCase("ADMIN@URBAN.PE"))
            .hasValueSatisfying(user -> {
                assertThat(user.getTenantId()).isEqualTo(response.id());
                assertThat(user.getRoles()).extracting(Role::getName).containsExactly("TENANT_ADMIN");
            });
    }

    @Test
    void updatesTenantBaseDataAndRejectsDuplicateSlug() {
        TenantDetailResponse first = tenantService.create(createRequest("urban-one", "one@urban.pe"));
        tenantService.create(createRequest("urban-two", "two@urban.pe"));

        assertThatThrownBy(() -> tenantService.update(
            first.id(),
            new UpdateTenantRequest("Urban One Updated", "urban-two", "PRO")
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Slug ya esta tomado");

        assertThat(tenantService.update(
            first.id(),
            new UpdateTenantRequest(" Urban One Updated ", "urban-one-new", "PRO")
        ).name()).isEqualTo("Urban One Updated");
    }

    @Test
    void changingRucResetsDocumentVerification() {
        TenantDetailResponse tenant = tenantService.create(createRequest("urban-ruc", "ruc@urban.pe"));
        TenantBusinessInfo info = businessInfoRepository.findByTenantId(tenant.id()).orElseThrow();
        info.setDocumentStatus("VERIFIED");
        businessInfoRepository.saveAndFlush(info);

        tenantService.updateBusinessInfo(
            tenant.id(),
            new UpdateBusinessInfoRequest(
                "EMPRESA",
                "Urban Style",
                "Urban Style SAC",
                "20999999999",
                "987654321",
                "VENTAS@URBAN.PE",
                "Av. Lima 123",
                "Miraflores",
                "Lima",
                "Lima"
            )
        );

        assertThat(businessInfoRepository.findByTenantId(tenant.id()).orElseThrow().getDocumentStatus())
            .isEqualTo("PENDING_VERIFICATION");
    }

    @Test
    void rejectsInactiveTemplateAndHidesSuspendedStore() {
        TenantDetailResponse tenant = tenantService.create(createRequest("urban-hidden", "hidden@urban.pe"));
        StoreTemplate template = new StoreTemplate();
        template.setName("Inactive");
        template.setCode("INACTIVE");
        template.setActive(false);
        template = templateRepository.save(template);
        UUID templateId = template.getId();

        assertThatThrownBy(() -> tenantService.updateSettings(
            tenant.id(),
            new UpdateTenantSettingsRequest(
                templateId,
                "https://cdn.example.com/logo.png",
                null,
                "#000000",
                "#FFFFFF",
                "#FF00AA",
                "Inter",
                "987654321",
                null,
                null,
                null
            )
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Plantilla no esta activa");

        tenantService.suspend(tenant.id());

        assertThatThrownBy(() -> tenantService.getPublicBySlug("urban-hidden"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsCompanyWithoutRuc() {
        CreateTenantRequest request = new CreateTenantRequest(
            "Urban Invalid",
            "urban-invalid",
            "BASIC",
            new BusinessInfoRequest(
                "EMPRESA",
                "Urban Invalid",
                "Urban Invalid SAC",
                null,
                "987654321",
                "invalid@urban.pe",
                null,
                null,
                null,
                null
            ),
            new InitialAdminRequest(
                "Admin Invalid",
                "invalid-admin@urban.pe",
                "Password123!",
                "987654321"
            )
        );

        assertThatThrownBy(() -> tenantService.create(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("RUC obligatorio");
    }

    private CreateTenantRequest createRequest(String slug, String adminEmail) {
        return new CreateTenantRequest(
            " Urban Style ",
            slug,
            "BASIC",
            new BusinessInfoRequest(
                "EMPRESA",
                " Urban Style ",
                " Urban Style SAC ",
                "20123456789",
                "987654321",
                " VENTAS@URBAN.PE ",
                " Av. Lima 123 ",
                " Miraflores ",
                " Lima ",
                " Lima "
            ),
            new InitialAdminRequest(
                " Tenant Admin ",
                adminEmail,
                "Password123!",
                "987654321"
            )
        );
    }
}
