package com.urban_shop.backend.common.security;

import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.customer.entity.Customer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    @PostConstruct
    void validateConfiguration() {
        signingKey();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String generateToken(User user, List<String> roles) {
        return generateToken(
            user.getId(),
            user.getTenantId(),
            user.getEmail(),
            roles,
            PrincipalType.INTERNAL_USER
        );
    }

    public String generateToken(Customer customer) {
        return generateToken(
            customer.getId(),
            customer.getTenantId(),
            customer.getEmail(),
            List.of("CUSTOMER"),
            PrincipalType.CUSTOMER
        );
    }

    public String generateToken(
        UUID principalId,
        UUID tenantId,
        String email,
        List<String> roles,
        PrincipalType principalType
    ) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getExpirationMs());
        return Jwts.builder()
            .subject(principalId.toString())
            .claim("tenant_id", tenantId != null ? tenantId.toString() : null)
            .claim("roles", roles)
            .claim("email", email)
            .claim("principal_type", principalType.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .issuer(properties.getIssuer())
            .signWith(signingKey())
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .requireIssuer(properties.getIssuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
