package com.urban_shop.backend.auth.service;

import com.urban_shop.backend.auth.dto.request.ChangePasswordRequest;
import com.urban_shop.backend.auth.dto.request.ForgotPasswordRequest;
import com.urban_shop.backend.auth.dto.request.LoginRequest;
import com.urban_shop.backend.auth.dto.request.RefreshTokenRequest;
import com.urban_shop.backend.auth.dto.request.RegisterCustomerRequest;
import com.urban_shop.backend.auth.dto.request.ResendVerificationRequest;
import com.urban_shop.backend.auth.dto.request.ResetPasswordRequest;
import com.urban_shop.backend.auth.dto.request.VerifyEmailRequest;
import com.urban_shop.backend.auth.dto.response.LoginResponse;
import com.urban_shop.backend.auth.dto.response.UserInfoResponse;
import com.urban_shop.backend.auth.entity.EmailVerificationToken;
import com.urban_shop.backend.auth.entity.PasswordResetToken;
import com.urban_shop.backend.auth.entity.RefreshToken;
import com.urban_shop.backend.auth.entity.ResetPrincipalType;
import com.urban_shop.backend.auth.repository.EmailVerificationTokenRepository;
import com.urban_shop.backend.auth.repository.PasswordResetTokenRepository;
import com.urban_shop.backend.auth.repository.RefreshTokenRepository;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.common.security.JwtProperties;
import com.urban_shop.backend.common.security.JwtService;
import com.urban_shop.backend.common.security.PrincipalType;
import com.urban_shop.backend.common.util.TokenHashes;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import com.urban_shop.backend.customer.service.CustomerService;
import com.urban_shop.backend.email.service.EmailService;
import com.urban_shop.backend.tenant.service.PublicTenantResolver;
import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long RESET_TOKEN_TTL_MINUTES = 15;
    private static final long REFRESH_TOKEN_TTL_DAYS = 30;
    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;
    private static final String INVALID_RESET_TOKEN = "Token de restablecimiento invalido o expirado";
    private static final String CUSTOMER_NOT_FOUND = "Cliente no encontrado";
    private static final String USER_NOT_FOUND = "Usuario no encontrado";

    private final AuthenticationManager authenticationManager;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final PublicTenantResolver publicTenantResolver;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final EmailService emailService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    @Override
    @Transactional
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
        log.info("SECURITY AUDIT: Successful login for user {} [type: {}]", principal.getUsername(), principal.getPrincipalType());
        String refreshToken = issueRefreshToken(
            principal.getPrincipalId(),
            principal.getTenantId(),
            ResetPrincipalType.USER
        );
        return loginResponse(token, refreshToken, toUserInfo(principal));
    }

    @Override
    @Transactional
    public LoginResponse registerCustomer(RegisterCustomerRequest request) {
        UUID tenantId = publicTenantResolver.requireTenantId(request.tenantSlug());
        Customer customer = customerService.register(tenantId, request);
        log.info("SECURITY AUDIT: New customer registered: {} under tenantId: {}", customer.getEmail(), tenantId);
        if (customer.getEmail() != null) {
            issueVerificationToken(
                customer.getEmail().toLowerCase(),
                tenantId,
                ResetPrincipalType.CUSTOMER,
                LocalDateTime.now()
            );
        }
        String refreshToken = issueRefreshToken(customer.getId(), tenantId, ResetPrincipalType.CUSTOMER);
        return loginResponse(jwtService.generateToken(customer), refreshToken, toCustomerInfo(customer));
    }

    @Override
    public UserInfoResponse me(CustomUserDetails principal) {
        return toUserInfo(principal);
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        LocalDateTime now = LocalDateTime.now();
        RefreshToken stored = refreshTokenRepository
            .findByTokenHash(TokenHashes.sha256(request.refreshToken()))
            .filter(token -> token.isUsable(now))
            .orElseThrow(() -> {
                log.warn("SECURITY AUDIT: intento de refresh con token invalido o revocado");
                return new BadCredentialsException("Refresh token invalido o expirado");
            });

        // Rotacion: el token usado se revoca y se emite uno nuevo.
        stored.setRevokedAt(now);
        refreshTokenRepository.save(stored);

        if (stored.getPrincipalType() == ResetPrincipalType.CUSTOMER) {
            Customer customer = customerRepository.findById(stored.getPrincipalId())
                .filter(Customer::isActive)
                .orElseThrow(() -> new BadCredentialsException("Cliente no disponible"));
            String refreshToken = issueRefreshToken(customer.getId(), customer.getTenantId(), ResetPrincipalType.CUSTOMER);
            return loginResponse(jwtService.generateToken(customer), refreshToken, toCustomerInfo(customer));
        }

        User user = userRepository.findById(stored.getPrincipalId())
            .filter(User::isActive)
            .orElseThrow(() -> new BadCredentialsException("Usuario no disponible"));
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        String token = jwtService.generateToken(user, roles);
        String refreshToken = issueRefreshToken(user.getId(), user.getTenantId(), ResetPrincipalType.USER);
        return loginResponse(token, refreshToken, toUserInfo(user, roles));
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenHash(TokenHashes.sha256(request.refreshToken()))
            .ifPresent(token -> {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
                log.info("SECURITY AUDIT: sesion cerrada para el principal {}", token.getPrincipalId());
            });
    }

    @Override
    @Transactional
    public void changePassword(CustomUserDetails principal, ChangePasswordRequest request) {
        if (principal == null) {
            throw new BadCredentialsException("Se requiere un usuario autenticado");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("La nueva contrasena debe ser distinta de la actual");
        }

        String encoded = passwordEncoder.encode(request.newPassword());
        if (principal.isCustomer()) {
            Customer customer = customerRepository.findById(principal.getPrincipalId())
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND));
            requireCurrentPassword(request.currentPassword(), customer.getPasswordHash());
            customer.setPasswordHash(encoded);
            customerRepository.save(customer);
        } else {
            User user = userRepository.findById(principal.getPrincipalId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
            requireCurrentPassword(request.currentPassword(), user.getPasswordHash());
            user.setPasswordHash(encoded);
            userRepository.save(user);
        }

        refreshTokenRepository.revokeAllForPrincipal(principal.getPrincipalId(), LocalDateTime.now());
        log.info("SECURITY AUDIT: contrasena actualizada para {}", principal.getUsername());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        if (request.tenantSlug() != null && !request.tenantSlug().isBlank()) {
            UUID tenantId = publicTenantResolver.requireTenantId(request.tenantSlug());
            customerRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email).ifPresent(customer ->
                issueResetToken(email, tenantId, ResetPrincipalType.CUSTOMER, now)
            );
        } else {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user ->
                issueResetToken(email, user.getTenantId(), ResetPrincipalType.USER, now)
            );
        }
        // Respuesta identica exista o no la cuenta, para no permitir enumeracion de correos.
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = resetTokenRepository
            .findByTokenHash(TokenHashes.sha256(request.token().trim()))
            .filter(candidate -> candidate.isUsable(now))
            .orElseThrow(() -> new BusinessException(INVALID_RESET_TOKEN));

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        UUID principalId;
        if (token.getPrincipalType() == ResetPrincipalType.CUSTOMER) {
            Customer customer = customerRepository
                .findByTenantIdAndEmailIgnoreCase(token.getTenantId(), token.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND));
            customer.setPasswordHash(encodedPassword);
            customerRepository.save(customer);
            principalId = customer.getId();
            log.info("SECURITY AUDIT: Customer password successfully reset for: {}", token.getEmail());
        } else {
            User user = userRepository.findByEmailIgnoreCase(token.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
            user.setPasswordHash(encodedPassword);
            userRepository.save(user);
            principalId = user.getId();
            log.info("SECURITY AUDIT: Admin user password successfully reset for: {}", token.getEmail());
        }

        token.setUsedAt(now);
        resetTokenRepository.save(token);
        refreshTokenRepository.revokeAllForPrincipal(principalId, now);
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationToken token = verificationTokenRepository
            .findByTokenHash(TokenHashes.sha256(request.token().trim()))
            .filter(candidate -> candidate.isUsable(now))
            .orElseThrow(() -> new BusinessException("Token de verificacion invalido o expirado"));

        if (token.getPrincipalType() == ResetPrincipalType.CUSTOMER) {
            Customer customer = customerRepository
                .findByTenantIdAndEmailIgnoreCase(token.getTenantId(), token.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND));
            customer.setEmailVerified(true);
            customerRepository.save(customer);
        } else {
            User user = userRepository.findByEmailIgnoreCase(token.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        token.setUsedAt(now);
        verificationTokenRepository.save(token);
        log.info("SECURITY AUDIT: correo verificado para {}", token.getEmail());
    }

    @Override
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        UUID tenantId = publicTenantResolver.requireTenantId(request.tenantSlug());
        String email = request.email().trim().toLowerCase();

        customerRepository.findByTenantIdAndEmailIgnoreCase(tenantId, email)
            .filter(customer -> !customer.isEmailVerified())
            .ifPresent(customer -> issueVerificationToken(
                email,
                tenantId,
                ResetPrincipalType.CUSTOMER,
                LocalDateTime.now()
            ));
        // Respuesta identica exista o no la cuenta, para no permitir enumeracion de correos.
    }

    private void issueVerificationToken(
        String email,
        UUID tenantId,
        ResetPrincipalType principalType,
        LocalDateTime now
    ) {
        verificationTokenRepository.invalidateActiveTokens(email, now);

        String rawToken = TokenHashes.randomToken();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail(email);
        token.setTenantId(tenantId);
        token.setPrincipalType(principalType);
        token.setTokenHash(TokenHashes.sha256(rawToken));
        token.setExpiresAt(now.plusHours(VERIFICATION_TOKEN_TTL_HOURS));
        verificationTokenRepository.save(token);

        emailService.sendEmailVerificationEmail(email, rawToken);
    }

    private void issueResetToken(String email, UUID tenantId, ResetPrincipalType principalType, LocalDateTime now) {
        resetTokenRepository.invalidateActiveTokens(email, now);

        String rawToken = TokenHashes.randomToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setTenantId(tenantId);
        token.setPrincipalType(principalType);
        token.setTokenHash(TokenHashes.sha256(rawToken));
        token.setExpiresAt(now.plusMinutes(RESET_TOKEN_TTL_MINUTES));
        resetTokenRepository.save(token);

        emailService.sendPasswordResetEmail(email, rawToken);
        log.info("SECURITY AUDIT: Password reset token generated for {} [{}]", email, principalType);
    }

    private String issueRefreshToken(UUID principalId, UUID tenantId, ResetPrincipalType principalType) {
        String rawToken = TokenHashes.randomToken();
        RefreshToken token = new RefreshToken();
        token.setPrincipalId(principalId);
        token.setTenantId(tenantId);
        token.setPrincipalType(principalType);
        token.setTokenHash(TokenHashes.sha256(rawToken));
        token.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS));
        refreshTokenRepository.save(token);
        return rawToken;
    }

    private void requireCurrentPassword(String rawPassword, String storedHash) {
        if (storedHash == null || !passwordEncoder.matches(rawPassword, storedHash)) {
            throw new BadCredentialsException("La contrasena actual no es correcta");
        }
    }

    private LoginResponse loginCustomer(LoginRequest request) {
        UUID tenantId = publicTenantResolver.requireTenantId(request.tenantSlug());
        Customer customer = customerRepository
            .findByTenantIdAndEmailIgnoreCase(tenantId, request.email().trim())
            .orElseThrow(() -> {
                log.warn("SECURITY AUDIT: Failed customer login attempt for email {} on tenantId {}", request.email(), tenantId);
                return new BadCredentialsException("Credenciales invalidas");
            });

        if (!customer.isActive()
            || customer.getPasswordHash() == null
            || !passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            log.warn("SECURITY AUDIT: Invalid password or inactive customer login attempt for email {} on tenantId {}", request.email(), tenantId);
            throw new BadCredentialsException("Credenciales invalidas");
        }
        log.info("SECURITY AUDIT: Successful customer login for email {} on tenantId {}", customer.getEmail(), tenantId);
        String refreshToken = issueRefreshToken(customer.getId(), tenantId, ResetPrincipalType.CUSTOMER);
        return loginResponse(jwtService.generateToken(customer), refreshToken, toCustomerInfo(customer));
    }

    private LoginResponse loginResponse(String token, String refreshToken, UserInfoResponse userInfo) {
        return new LoginResponse(
            token,
            "Bearer",
            jwtProperties.getExpirationMs() / 1000,
            refreshToken,
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

    private UserInfoResponse toUserInfo(User user, List<String> roles) {
        return new UserInfoResponse(
            user.getId(),
            user.getTenantId(),
            user.getEmail(),
            user.getFullName(),
            Set.copyOf(roles),
            PrincipalType.INTERNAL_USER.name()
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
