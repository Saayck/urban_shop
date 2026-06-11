package com.urban_shop.backend.common.security;

import com.urban_shop.backend.common.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHENTICATION_ERROR_ATTRIBUTE =
        JwtAuthenticationFilter.class.getName() + ".authenticationError";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);
                UUID userId = parseUserId(claims.getSubject());
                String email = claims.get("email", String.class);
                PrincipalType principalType = parsePrincipalType(claims);

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    CustomUserDetails userDetails = principalType == PrincipalType.CUSTOMER
                        ? userDetailsService.loadPrincipalById(userId, principalType)
                        : userDetailsService.loadUserById(userId);
                    validateClaims(claims, email, principalType, userDetails);
                    if (!userDetails.isEnabled()) {
                        throw new DisabledException("Usuario o tenant deshabilitado");
                    }

                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    if (userDetails.getTenantId() != null) {
                        TenantContext.setTenantId(userDetails.getTenantId());
                    }
                }
            } catch (JwtException | AuthenticationException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                TenantContext.clear();
                request.setAttribute(AUTHENTICATION_ERROR_ATTRIBUTE, "Token invalido, expirado o revocado");
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID parseUserId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new JwtException("JWT subject is required");
        }
        return UUID.fromString(subject);
    }

    private PrincipalType parsePrincipalType(Claims claims) {
        String value = claims.get("principal_type", String.class);
        if (value == null || value.isBlank()) {
            throw new JwtException("JWT principal type is required");
        }
        return PrincipalType.valueOf(value);
    }

    private void validateClaims(
        Claims claims,
        String email,
        PrincipalType principalType,
        CustomUserDetails userDetails
    ) {
        if (email == null || !email.equalsIgnoreCase(userDetails.getUsername())) {
            throw new JwtException("JWT email does not match current user");
        }
        if (principalType != userDetails.getPrincipalType()) {
            throw new JwtException("JWT principal type does not match current principal");
        }

        String tenantId = claims.get("tenant_id", String.class);
        UUID currentTenantId = userDetails.getTenantId();
        if (currentTenantId == null) {
            if (tenantId != null && !tenantId.isBlank()) {
                throw new JwtException("JWT tenant does not match current user");
            }
        } else if (!currentTenantId.toString().equals(tenantId)) {
            throw new JwtException("JWT tenant does not match current user");
        }

        List<?> tokenRoles = claims.get("roles", List.class);
        if (tokenRoles == null) {
            throw new JwtException("JWT roles are required");
        }
        Set<String> claimedRoles = new HashSet<>();
        for (Object role : tokenRoles) {
            if (!(role instanceof String roleName)) {
                throw new JwtException("JWT roles are invalid");
            }
            claimedRoles.add(roleName);
        }
        Set<String> currentRoles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(authority -> authority.replaceFirst("^ROLE_", ""))
            .collect(java.util.stream.Collectors.toSet());
        if (!claimedRoles.equals(currentRoles)) {
            throw new JwtException("JWT roles do not match current user");
        }
    }
}
