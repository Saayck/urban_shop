package com.urban_shop.backend.auth;

import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.request.ResendVerificationRequest;
import com.urban_shop.backend.auth.dto.request.VerifyEmailRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.repository.EmailVerificationTokenRepository;
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.util.TokenHashes;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import com.urban_shop.backend.email.service.EmailService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class EmailVerificationIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @MockBean
    private EmailService emailService;

    @Test
    void sendsAVerificationTokenOnRegistrationAndConfirmsTheEmail() {
        String slug = "verify-flow";
        createTenant(slug);
        String email = slug + "@urban.pe";

        LoginResponse registered = registerCustomer(slug, email);
        assertThat(customerRepository.findById(registered.user().id()).orElseThrow().isEmailVerified())
            .isFalse();

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmailVerificationEmail(eq(email), tokenCaptor.capture());
        String rawToken = tokenCaptor.getValue();

        // En base de datos vive el hash, no el token en claro.
        assertThat(verificationTokenRepository.findByTokenHash(TokenHashes.sha256(rawToken))).isPresent();
        assertThat(verificationTokenRepository.findByTokenHash(rawToken)).isEmpty();

        authService.verifyEmail(new VerifyEmailRequest(rawToken));

        assertThat(customerRepository.findById(registered.user().id()).orElseThrow().isEmailVerified())
            .isTrue();

        // El token es de un solo uso.
        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest(rawToken)))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsAnUnknownToken() {
        assertThatThrownBy(() -> authService.verifyEmail(new VerifyEmailRequest("token-inventado")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("invalido o expirado");
    }

    @Test
    void resendsOnlyForUnverifiedAccountsAndNeverLeaksExistence() {
        String slug = "verify-resend";
        createTenant(slug);
        String email = slug + "@urban.pe";
        registerCustomer(slug, email);

        authService.resendVerification(new ResendVerificationRequest(slug, email));
        // Uno del registro y otro del reenvio.
        verify(emailService, org.mockito.Mockito.times(2))
            .sendEmailVerificationEmail(eq(email), anyString());

        // Una cuenta inexistente no genera correo ni error.
        authService.resendVerification(new ResendVerificationRequest(slug, "fantasma@urban.pe"));
        verify(emailService, never()).sendEmailVerificationEmail(eq("fantasma@urban.pe"), anyString());
    }

    private LoginResponse registerCustomer(String slug, String email) {
        return authService.registerCustomer(new RegisterCustomerRequest(
            slug, "Cliente", "Prueba", email, "987654321", "Password123!", null, null
        ));
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
