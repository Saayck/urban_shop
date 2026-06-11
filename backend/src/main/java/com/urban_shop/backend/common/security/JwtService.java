package com.urban_shop.backend.common.security;

import com.urban_shop.backend.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String generateToken(User user, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getExpirationMs());
        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("tenant_id", user.getTenantId() != null ? user.getTenantId().toString() : null)
            .claim("roles", roles)
            .claim("email", user.getEmail())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey())
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
