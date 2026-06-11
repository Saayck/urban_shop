package com.urban_shop.backend.customer;

import com.urban_shop.backend.auth.dto.request.LoginRequest;
import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.security.JwtService;
import com.urban_shop.backend.customer.dto.request.CustomerAddressRequest;
import com.urban_shop.backend.customer.dto.response.CustomerAddressResponse;
import com.urban_shop.backend.customer.service.CustomerService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CustomerFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registersAndAuthenticatesCustomerWithinTenant() throws Exception {
        Tenant firstTenant = createTenant("customer-one");
        createTenant("customer-two");
        RegisterCustomerRequest request = registration("customer-one", "cliente@urban.pe", "987654321");

        LoginResponse registration = authService.registerCustomer(request);
        Claims claims = jwtService.parse(registration.token());

        assertThat(claims.get("principal_type", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.get("tenant_id", String.class)).isEqualTo(firstTenant.getId().toString());
        assertThat(claims.get("roles", List.class)).containsExactly("CUSTOMER");

        LoginResponse login = authService.login(
            new LoginRequest("CLIENTE@URBAN.PE", "Password123!", "customer-one")
        );
        assertThat(login.user().principalType()).isEqualTo("CUSTOMER");

        mockMvc.perform(get("/api/customer/me")
                .header("Authorization", "Bearer " + login.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("cliente@urban.pe"))
            .andExpect(jsonPath("$.tenantId").value(firstTenant.getId().toString()));

        mockMvc.perform(get("/api/admin/brands")
                .header("Authorization", "Bearer " + login.token()))
            .andExpect(status().isForbidden());

        authService.registerCustomer(
            registration("customer-two", "cliente@urban.pe", "976543219")
        );
        assertThatThrownBy(() -> authService.registerCustomer(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("correo ya esta registrado");
    }

    @Test
    void managesSingleDefaultAddress() {
        createTenant("address-store");
        LoginResponse login = authService.registerCustomer(
            registration("address-store", "address@urban.pe", "987654322")
        );

        CustomerAddressResponse first = customerService.createAddress(
            login.user().tenantId(),
            login.user().id(),
            address("Miraflores", false)
        );
        CustomerAddressResponse second = customerService.createAddress(
            login.user().tenantId(),
            login.user().id(),
            address("Barranco", true)
        );

        assertThat(first.defaultAddress()).isTrue();
        assertThat(customerService.listAddresses(login.user().tenantId(), login.user().id()))
            .filteredOn(CustomerAddressResponse::defaultAddress)
            .extracting(CustomerAddressResponse::id)
            .containsExactly(second.id());

        customerService.deleteAddress(login.user().tenantId(), login.user().id(), second.id());
        assertThat(customerService.listAddresses(login.user().tenantId(), login.user().id()))
            .singleElement()
            .satisfies(address -> assertThat(address.defaultAddress()).isTrue());
    }

    private RegisterCustomerRequest registration(String slug, String email, String phone) {
        return new RegisterCustomerRequest(
            slug,
            "Ana",
            "Torres",
            email,
            phone,
            "Password123!",
            "DNI",
            "12345678"
        );
    }

    private CustomerAddressRequest address(String district, boolean defaultAddress) {
        return new CustomerAddressRequest(
            "Lima",
            "Lima",
            district,
            "Av. Principal 123",
            "Frente al parque",
            new BigDecimal("-12.1212121"),
            new BigDecimal("-77.0303030"),
            defaultAddress
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
