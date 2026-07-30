package com.urban_shop.backend.auth;

import com.urban_shop.backend.auth.dto.request.ChangePasswordRequest;
import com.urban_shop.backend.auth.dto.request.ForgotPasswordRequest;
import com.urban_shop.backend.auth.dto.request.LoginRequest;
import com.urban_shop.backend.auth.dto.request.RefreshTokenRequest;
import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.request.ResetPasswordRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.entity.PasswordResetToken;
import com.urban_shop.backend.auth.repository.PasswordResetTokenRepository;
import com.urban_shop.backend.auth.repository.RefreshTokenRepository;
import com.urban_shop.backend.auth.service.AuthService;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.common.util.TokenHashes;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import com.urban_shop.backend.email.service.EmailService;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class AuthTokenIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @MockBean
    private EmailService emailService;

    @Test
    void rotatesRefreshTokenAndRevokesThePreviousOne() {
        String slug = "auth-refresh";
        createTenant(slug);
        LoginResponse registered = registerCustomer(slug, slug + "@urban.pe");

        assertThat(registered.refreshToken()).isNotBlank();

        LoginResponse refreshed = authService.refresh(new RefreshTokenRequest(registered.refreshToken()));
        assertThat(refreshed.refreshToken())
            .isNotBlank()
            .isNotEqualTo(registered.refreshToken());

        // El token ya usado queda revocado: reutilizarlo debe fallar.
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(registered.refreshToken())))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        String slug = "auth-logout";
        createTenant(slug);
        LoginResponse registered = registerCustomer(slug, slug + "@urban.pe");

        authService.logout(new RefreshTokenRequest(registered.refreshToken()));

        assertThat(refreshTokenRepository.findByTokenHash(TokenHashes.sha256(registered.refreshToken())))
            .isPresent()
            .hasValueSatisfying(token -> assertThat(token.getRevokedAt()).isNotNull());
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(registered.refreshToken())))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void resetsPasswordWithAPersistedSingleUseToken() {
        String slug = "auth-reset";
        createTenant(slug);
        String email = slug + "@urban.pe";
        registerCustomer(slug, email);

        authService.forgotPassword(new ForgotPasswordRequest(slug, email));

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(email), tokenCaptor.capture());
        String rawToken = tokenCaptor.getValue();

        // En base de datos solo vive el hash, nunca el token en claro.
        PasswordResetToken stored = resetTokenRepository.findByTokenHash(TokenHashes.sha256(rawToken))
            .orElseThrow();
        assertThat(stored.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(stored.isUsable(LocalDateTime.now())).isTrue();

        authService.resetPassword(new ResetPasswordRequest(rawToken, "NuevaClave123!"));

        LoginResponse login = authService.login(new LoginRequest(email, "NuevaClave123!", slug));
        assertThat(login.token()).isNotBlank();

        // El token es de un solo uso.
        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest(rawToken, "OtraClave123!")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void forgotPasswordDoesNotLeakWhetherTheAccountExists() {
        String slug = "auth-enumeration";
        createTenant(slug);

        authService.forgotPassword(new ForgotPasswordRequest(slug, "inexistente@urban.pe"));

        assertThat(resetTokenRepository.findAll())
            .noneMatch(token -> "inexistente@urban.pe".equals(token.getEmail()));
        verify(emailService, org.mockito.Mockito.never())
            .sendPasswordResetEmail(eq("inexistente@urban.pe"), anyString());
    }

    @Test
    void changingPasswordRequiresTheCurrentOneAndClosesOpenSessions() {
        String slug = "auth-change";
        createTenant(slug);
        String email = slug + "@urban.pe";
        LoginResponse registered = registerCustomer(slug, email);

        Customer customer = customerRepository.findById(registered.user().id()).orElseThrow();
        CustomUserDetails principal = new CustomUserDetails(customer, true);

        assertThatThrownBy(() -> authService.changePassword(
            principal,
            new ChangePasswordRequest("ClaveIncorrecta1!", "NuevaClave123!")
        )).isInstanceOf(BadCredentialsException.class);

        authService.changePassword(principal, new ChangePasswordRequest("Password123!", "NuevaClave123!"));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(registered.refreshToken())))
            .isInstanceOf(BadCredentialsException.class);
        assertThat(authService.login(new LoginRequest(email, "NuevaClave123!", slug)).token()).isNotBlank();
    }

    private LoginResponse registerCustomer(String slug, String email) {
        return authService.registerCustomer(new RegisterCustomerRequest(
            slug,
            "Cliente",
            "Prueba",
            email,
            "987654321",
            "Password123!",
            null,
            null
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
