package com.urban_shop.backend.user;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import com.urban_shop.backend.user.dto.request.StaffUserCreateRequest;
import com.urban_shop.backend.user.dto.request.StaffUserUpdateRequest;
import com.urban_shop.backend.user.dto.response.StaffUserResponse;
import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.repository.RoleRepository;
import com.urban_shop.backend.user.service.StaffUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class StaffUserIntegrationTest {

    @Autowired
    private StaffUserService staffUserService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void ensureRoles() {
        createRoleIfMissing("TENANT_ADMIN");
        createRoleIfMissing("SALES_STAFF");
    }

    @Test
    void createsListsAndUpdatesStaff() {
        UUID tenantId = createTenant("staff-crud").getId();

        StaffUserResponse admin = staffUserService.create(tenantId, request("admin@staff-crud.pe", "TENANT_ADMIN"));
        StaffUserResponse seller = staffUserService.create(tenantId, request("ventas@staff-crud.pe", "SALES_STAFF"));

        assertThat(admin.roles()).containsExactly("TENANT_ADMIN");
        assertThat(seller.active()).isTrue();
        assertThat(staffUserService.list(tenantId)).hasSize(2);
        assertThat(staffUserService.get(tenantId, seller.id()).email()).isEqualTo("ventas@staff-crud.pe");

        StaffUserResponse promoted = staffUserService.update(
            tenantId,
            admin.id(),
            seller.id(),
            new StaffUserUpdateRequest("Vendedor Promovido", "987654321", "TENANT_ADMIN", true)
        );
        assertThat(promoted.roles()).containsExactly("TENANT_ADMIN");
        assertThat(promoted.fullName()).isEqualTo("Vendedor Promovido");
    }

    @Test
    void rejectsDuplicateEmails() {
        UUID tenantId = createTenant("staff-dup").getId();
        staffUserService.create(tenantId, request("repetido@staff-dup.pe", "SALES_STAFF"));

        assertThatThrownBy(() -> staffUserService.create(tenantId, request("repetido@staff-dup.pe", "SALES_STAFF")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Ya existe un usuario");
    }

    @Test
    void deactivatesStaffButNeverTheLastAdmin() {
        UUID tenantId = createTenant("staff-deactivate").getId();
        StaffUserResponse admin = staffUserService.create(tenantId, request("admin@staff-deactivate.pe", "TENANT_ADMIN"));
        StaffUserResponse seller = staffUserService.create(tenantId, request("ventas@staff-deactivate.pe", "SALES_STAFF"));

        staffUserService.deactivate(tenantId, admin.id(), seller.id());
        assertThat(staffUserService.get(tenantId, seller.id()).active()).isFalse();

        StaffUserResponse other = staffUserService.create(tenantId, request("otro@staff-deactivate.pe", "SALES_STAFF"));
        assertThatThrownBy(() -> staffUserService.deactivate(tenantId, other.id(), admin.id()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("al menos un TENANT_ADMIN activo");

        assertThatThrownBy(() -> staffUserService.deactivate(tenantId, admin.id(), admin.id()))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("no puede desactivarse a si mismo");
    }

    @Test
    void doesNotExposeStaffFromAnotherTenant() {
        UUID tenantA = createTenant("staff-tenant-a").getId();
        UUID tenantB = createTenant("staff-tenant-b").getId();
        StaffUserResponse userOfA = staffUserService.create(tenantA, request("a@staff-tenant.pe", "SALES_STAFF"));

        assertThatThrownBy(() -> staffUserService.get(tenantB, userOfA.id()))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThat(staffUserService.list(tenantB)).isEmpty();
    }

    private StaffUserCreateRequest request(String email, String role) {
        return new StaffUserCreateRequest("Usuario Interno", email, "Password123!", "987654321", role);
    }

    private void createRoleIfMissing(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
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
