package com.urban_shop.backend.audit.service;

import com.urban_shop.backend.audit.dto.response.AuditLogResponse;
import com.urban_shop.backend.audit.entity.AuditLog;
import com.urban_shop.backend.audit.repository.AuditLogRepository;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.common.security.CustomUserDetails;
import com.urban_shop.backend.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private static final int MAX_VALUE_LENGTH = 4000;

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityName, UUID entityId, String oldValue, String newValue) {
        try {
            AuditLog entry = new AuditLog();
            entry.setTenantId(TenantContext.getTenantId());
            entry.setUserId(currentInternalUserId());
            entry.setAction(action);
            entry.setEntityName(entityName);
            entry.setEntityId(entityId);
            entry.setOldValue(truncate(oldValue));
            entry.setNewValue(truncate(newValue));

            HttpServletRequest request = currentRequest();
            if (request != null) {
                entry.setIpAddress(clientIp(request));
                entry.setUserAgent(truncate(request.getHeader("User-Agent")));
            }
            auditLogRepository.save(entry);
        } catch (RuntimeException ex) {
            // La auditoria no debe tumbar la operacion de negocio.
            log.error("No se pudo registrar la auditoria de la accion {}", action, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(
        UUID tenantId,
        String action,
        String entityName,
        LocalDate from,
        LocalDate to,
        int page,
        int size
    ) {
        Page<AuditLog> logs = auditLogRepository.search(
            tenantId,
            blankToNull(action),
            blankToNull(entityName),
            from == null ? null : from.atStartOfDay(),
            to == null ? null : to.plusDays(1).atTime(LocalTime.MIDNIGHT),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(logs.map(this::toResponse));
    }

    /** Solo los usuarios internos existen en {@code users}; para clientes la columna queda nula. */
    private UUID currentInternalUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return null;
        }
        return principal.isCustomer() ? null : principal.getPrincipalId();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncateTo(forwarded.split(",")[0].trim(), 50);
        }
        return truncateTo(request.getRemoteAddr(), 50);
    }

    private AuditLogResponse toResponse(AuditLog entry) {
        return new AuditLogResponse(
            entry.getId(),
            entry.getTenantId(),
            entry.getUserId(),
            entry.getAction(),
            entry.getEntityName(),
            entry.getEntityId(),
            entry.getOldValue(),
            entry.getNewValue(),
            entry.getIpAddress(),
            entry.getUserAgent(),
            entry.getCreatedAt()
        );
    }

    private String truncate(String value) {
        return truncateTo(value, MAX_VALUE_LENGTH);
    }

    private String truncateTo(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
