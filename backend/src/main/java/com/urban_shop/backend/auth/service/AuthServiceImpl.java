package com.urban_shop.backend.auth.service;

import com.urban_shop.backend.auth.dto.request.LoginRequest;
import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.dto.response.UserInfoResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.common.security.JwtProperties;
import com.urban_shop.backend.common.security.JwtService;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import com.urban_shop.backend.customer.service.CustomerService;
import com.urban_shop.backend.tenant.service.PublicTenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final PublicTenantResolver publicTenantResolver;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request.tenantSlug() != null && !request.tenantSlug().isBlank()) {
            return loginCustomer(request);
        }

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Set<String> roles = roles(principal);
        String token = jwtService.generateToken(
            principal.getPrincipalId(),
            principal.getTenantId(),
            principal.getUsername(),
            roles.stream().sorted().toList(),
            principal.getPrincipalType()
        );
        return loginResponse(token, toUserInfo(principal));
    }

    @Override
    @Transactional
    public LoginResponse registerCustomer(RegisterCustomerRequest request) {
        UUID tenantId = publicTenantResolver.requireTenantId(request.tenantSlug());
        Customer customer = customerService.register(tenantId, request);
        return loginResponse(jwtService.generateToken(customer), toCustomerInfo(customer));
    }

    @Override
    public UserInfoResponse me(CustomUserDetails principal) {
        return toUserInfo(principal);
    }

    private LoginResponse loginCustomer(LoginRequest request) {
        UUID tenantId = publicTenantResolver.requireTenantId(request.tenantSlug());
        Customer customer = customerRepository
            .findByTenantIdAndEmailIgnoreCase(tenantId, request.email().trim())
            .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (!customer.isActive()
            || customer.getPasswordHash() == null
            || !passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }
        return loginResponse(jwtService.generateToken(customer), toCustomerInfo(customer));
    }

    private LoginResponse loginResponse(String token, UserInfoResponse userInfo) {
        return new LoginResponse(
            token,
            "Bearer",
            jwtProperties.getExpirationMs() / 1000,
            userInfo
        );
    }

    private UserInfoResponse toUserInfo(CustomUserDetails principal) {
        return new UserInfoResponse(
            principal.getPrincipalId(),
            principal.getTenantId(),
            principal.getUsername(),
            principal.getFullName(),
            roles(principal),
            principal.getPrincipalType().name()
        );
    }

    private UserInfoResponse toCustomerInfo(Customer customer) {
        return new UserInfoResponse(
            customer.getId(),
            customer.getTenantId(),
            customer.getEmail(),
            customer.getFirstName() + " " + customer.getLastName(),
            Set.of("CUSTOMER"),
            "CUSTOMER"
        );
    }

    private Set<String> roles(CustomUserDetails principal) {
        return principal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(authority -> authority.replaceFirst("^ROLE_", ""))
            .collect(Collectors.toSet());
    }
}
