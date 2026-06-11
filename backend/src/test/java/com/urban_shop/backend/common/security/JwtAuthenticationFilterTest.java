package com.urban_shop.backend.common.security;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.urban_shop.backend.common.tenant.TenantContext;
import com.urban_shop.backend.user.entity.User;

import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTest {

    private CustomUserDetailsService userDetailsService;
    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    protected void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setExpirationMs(3600000);
        properties.setIssuer("urban-shop-test");
        jwtService = new JwtService(properties);
        jwtService.validateConfiguration();

        userDetailsService = mock(CustomUserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @AfterEach
    protected void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void authenticatesFromSubjectAndUsesCurrentTenant() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = JwtServiceTest.user(userId, tenantId, "admin@store.pe", "TENANT_ADMIN");
        when(userDetailsService.loadUserById(userId)).thenReturn(new CustomUserDetails(user, true));

        MockHttpServletRequest request = bearerRequest(
            jwtService.generateToken(user, List.of("TENANT_ADMIN"))
        );
        AtomicBoolean chainCalled = new AtomicBoolean();
        FilterChain chain = (servletRequest, servletResponse) -> {
            chainCalled.set(true);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
            assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chainCalled).isTrue();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void rejectsTokenWhenTenantClaimDoesNotMatchCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        User tokenUser = JwtServiceTest.user(
            userId,
            UUID.randomUUID(),
            "admin@store.pe",
            "TENANT_ADMIN"
        );
        User currentUser = JwtServiceTest.user(
            userId,
            UUID.randomUUID(),
            "admin@store.pe",
            "TENANT_ADMIN"
        );
        when(userDetailsService.loadUserById(userId)).thenReturn(new CustomUserDetails(currentUser, true));

        MockHttpServletRequest request = bearerRequest(
            jwtService.generateToken(tokenUser, List.of("TENANT_ADMIN"))
        );
        FilterChain chain = (servletRequest, servletResponse) ->
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTHENTICATION_ERROR_ATTRIBUTE))
            .isEqualTo("Token invalido, expirado o revocado");
    }

    @Test
    void rejectsTokenForDisabledUserOrTenant() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = JwtServiceTest.user(userId, UUID.randomUUID(), "admin@store.pe", "TENANT_ADMIN");
        when(userDetailsService.loadUserById(userId)).thenReturn(new CustomUserDetails(user, false));

        MockHttpServletRequest request = bearerRequest(
            jwtService.generateToken(user, List.of("TENANT_ADMIN"))
        );
        FilterChain chain = (servletRequest, servletResponse) ->
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(JwtAuthenticationFilter.AUTHENTICATION_ERROR_ATTRIBUTE))
            .isEqualTo("Token invalido, expirado o revocado");
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
