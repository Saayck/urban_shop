package com.urban_shop.backend.user.service;

import com.urban_shop.backend.audit.service.AuditService;
import com.urban_shop.backend.auth.repository.RefreshTokenRepository;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.user.dto.request.StaffUserCreateRequest;
import com.urban_shop.backend.user.dto.request.StaffUserUpdateRequest;
import com.urban_shop.backend.user.dto.response.StaffUserResponse;
import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.user.repository.RoleRepository;
import com.urban_shop.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffUserServiceImpl implements StaffUserService {

    private static final String USER_NOT_FOUND = "Usuario no encontrado";
    private static final String TENANT_ADMIN_ROLE = "TENANT_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public StaffUserResponse create(UUID tenantId, StaffUserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Ya existe un usuario con ese correo");
        }

        User user = new User();
        user.setTenantId(tenantId);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(trimToNull(request.phone()));
        user.setActive(true);
        user.getRoles().add(requireRole(request.role()));

        User saved = userRepository.save(user);
        auditService.record("STAFF_USER_CREATED", "User", saved.getId(), null, email + " con rol " + request.role());
        log.info("SECURITY AUDIT: usuario interno {} creado con rol {} en el tenant {}", email, request.role(), tenantId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffUserResponse> list(UUID tenantId) {
        return userRepository.findAllByTenantIdOrderByFullNameAsc(tenantId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffUserResponse get(UUID tenantId, UUID userId) {
        return toResponse(requireUser(tenantId, userId));
    }

    @Override
    @Transactional
    public StaffUserResponse update(
        UUID tenantId,
        UUID actingUserId,
        UUID userId,
        StaffUserUpdateRequest request
    ) {
        User user = requireUser(tenantId, userId);
        Role role = requireRole(request.role());
        boolean losingAdmin = hasRole(user, TENANT_ADMIN_ROLE) && !TENANT_ADMIN_ROLE.equals(role.getName());
        if ((losingAdmin || Boolean.FALSE.equals(request.active()))
            && isLastActiveAdmin(tenantId, userId)) {
            throw new BusinessException("La tienda debe conservar al menos un TENANT_ADMIN activo");
        }

        user.setFullName(request.fullName().trim());
        user.setPhone(trimToNull(request.phone()));
        user.getRoles().clear();
        user.getRoles().add(role);

        boolean wasActive = user.isActive();
        user.setActive(Boolean.TRUE.equals(request.active()));
        User saved = userRepository.save(user);

        if (wasActive && !saved.isActive()) {
            refreshTokenRepository.revokeAllForPrincipal(userId, LocalDateTime.now());
        }
        auditService.record("STAFF_USER_UPDATED", "User", userId, null, user.getEmail() + " rol " + role.getName() + " activo " + saved.isActive());
        log.info("SECURITY AUDIT: usuario interno {} actualizado por {}", user.getEmail(), actingUserId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID actingUserId, UUID userId) {
        if (userId.equals(actingUserId)) {
            throw new BusinessException("Un usuario no puede desactivarse a si mismo");
        }
        User user = requireUser(tenantId, userId);
        if (isLastActiveAdmin(tenantId, userId)) {
            throw new BusinessException("La tienda debe conservar al menos un TENANT_ADMIN activo");
        }

        user.setActive(false);
        userRepository.save(user);
        refreshTokenRepository.revokeAllForPrincipal(userId, LocalDateTime.now());
        auditService.record("STAFF_USER_DEACTIVATED", "User", userId, "activo", "inactivo");
        log.info("SECURITY AUDIT: usuario interno {} desactivado por {}", user.getEmail(), actingUserId);
    }

    /** Evita dejar la tienda sin ningun administrador con acceso. */
    private boolean isLastActiveAdmin(UUID tenantId, UUID userId) {
        return userRepository.findAllByTenantIdOrderByFullNameAsc(tenantId).stream()
            .filter(User::isActive)
            .filter(candidate -> hasRole(candidate, TENANT_ADMIN_ROLE))
            .allMatch(candidate -> candidate.getId().equals(userId));
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private Role requireRole(String roleName) {
        return roleRepository.findByName(roleName)
            .orElseThrow(() -> new BusinessException("Rol no valido: " + roleName));
    }

    private User requireUser(UUID tenantId, UUID userId) {
        return userRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
    }

    private StaffUserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        return new StaffUserResponse(
            user.getId(),
            user.getTenantId(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            user.isActive(),
            roles,
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
