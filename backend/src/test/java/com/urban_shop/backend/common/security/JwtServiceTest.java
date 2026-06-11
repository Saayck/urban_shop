package com.urban_shop.backend.common.security;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;

import io.jsonwebtoken.Claims;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    protected void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setExpirationMs(3600000);
        properties.setIssuer("urban-shop-test");
        jwtService = new JwtService(properties);
        jwtService.validateConfiguration();
    }

    @Test
    void generatesRequiredClaims() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        User user = user(userId, tenantId, "admin@store.pe", "TENANT_ADMIN");

        String token = jwtService.generateToken(user, List.of("TENANT_ADMIN"));
        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("tenant_id", String.class)).isEqualTo(tenantId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("admin@store.pe");
        assertThat(claims.get("roles", List.class)).containsExactly("TENANT_ADMIN");
        assertThat(claims.getIssuer()).isEqualTo("urban-shop-test");
    }

    static User user(UUID userId, UUID tenantId, String email, String roleName) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(roleName);

        User user = new User();
        user.setId(userId);
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFullName("Test User");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.getRoles().add(role);
        return user;
    }
}
