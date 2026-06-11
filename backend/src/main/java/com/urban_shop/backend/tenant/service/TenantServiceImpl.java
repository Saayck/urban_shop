package com.urban_shop.backend.tenant.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.tenant.dto.request.BusinessInfoRequest;
import com.urban_shop.backend.tenant.dto.request.CreateTenantRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateBusinessInfoRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateTenantRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateTenantSettingsRequest;
import com.urban_shop.backend.tenant.dto.response.StoreBusinessInfoResponse;
import com.urban_shop.backend.tenant.dto.response.StoreInfoResponse;
import com.urban_shop.backend.tenant.dto.response.TenantBusinessInfoResponse;
import com.urban_shop.backend.tenant.dto.response.TenantDetailResponse;
import com.urban_shop.backend.tenant.dto.response.TenantResponse;
import com.urban_shop.backend.tenant.dto.response.TenantSettingsResponse;
import com.urban_shop.backend.tenant.entity.Tenant;
import com.urban_shop.backend.tenant.entity.TenantBusinessInfo;
import com.urban_shop.backend.tenant.entity.TenantSettings;
import com.urban_shop.backend.tenant.repository.TenantBusinessInfoRepository;
import com.urban_shop.backend.tenant.repository.TenantRepository;
import com.urban_shop.backend.tenant.repository.TenantSettingsRepository;
import com.urban_shop.backend.template.entity.StoreTemplate;
import com.urban_shop.backend.template.repository.StoreTemplateRepository;
import com.urban_shop.backend.user.entity.Role;
import com.urban_shop.backend.user.entity.User;
import com.urban_shop.backend.user.repository.RoleRepository;
import com.urban_shop.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private static final Set<String> PUBLIC_STATUSES = Set.of("ACTIVE", "TRIAL");
    private static final Set<String> SUPPORTED_BUSINESS_TYPES = Set.of("EMPRESA", "PERSONA_NATURAL");

    private final TenantRepository tenantRepository;
    private final TenantBusinessInfoRepository businessInfoRepository;
    private final TenantSettingsRepository settingsRepository;
    private final StoreTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public TenantDetailResponse create(CreateTenantRequest request) {
        String slug = normalizeSlug(request.slug());
        if (tenantRepository.existsBySlug(slug)) {
            throw new BusinessException("Slug ya esta tomado: " + slug);
        }
        validateBusinessRule(request.businessInfo().businessType(), request.businessInfo().ruc());
        if (userRepository.existsByEmailIgnoreCase(request.initialAdmin().email())) {
            throw new BusinessException("Email ya esta tomado: " + request.initialAdmin().email());
        }

        Tenant tenant = new Tenant();
        tenant.setName(normalizeRequired(request.name()));
        tenant.setSlug(slug);
        tenant.setPlanName(normalizeUpper(request.planName()));
        tenant.setStatus("ACTIVE");
        tenant = tenantRepository.save(tenant);

        TenantBusinessInfo bi = new TenantBusinessInfo();
        bi.setTenantId(tenant.getId());
        applyBusinessInfo(bi, request.businessInfo());
        bi.setDocumentStatus("PENDING_VERIFICATION");
        bi = businessInfoRepository.save(bi);

        TenantSettings settings = new TenantSettings();
        settings.setTenantId(tenant.getId());
        settings = settingsRepository.save(settings);

        Role tenantAdminRole = roleRepository.findByName("TENANT_ADMIN")
            .orElseThrow(() -> new IllegalStateException("Role TENANT_ADMIN no encontrado"));

        User admin = new User();
        admin.setTenantId(tenant.getId());
        admin.setFullName(normalizeRequired(request.initialAdmin().fullName()));
        admin.setEmail(request.initialAdmin().email().trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(request.initialAdmin().password()));
        admin.setPhone(normalizeNullable(request.initialAdmin().phone()));
        admin.setActive(true);
        admin.getRoles().add(tenantAdminRole);
        userRepository.save(admin);

        return toDetailResponse(tenant, bi, settings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return tenantRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDetailResponse getById(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        TenantBusinessInfo bi = businessInfoRepository.findByTenantId(tenantId).orElse(null);
        TenantSettings settings = settingsRepository.findByTenantId(tenantId).orElse(null);
        return toDetailResponse(tenant, bi, settings);
    }

    @Override
    @Transactional
    public TenantResponse update(UUID tenantId, UpdateTenantRequest request) {
        Tenant tenant = requireTenant(tenantId);
        String slug = normalizeSlug(request.slug());
        if (tenantRepository.existsBySlugAndIdNot(slug, tenantId)) {
            throw new BusinessException("Slug ya esta tomado: " + slug);
        }

        tenant.setName(normalizeRequired(request.name()));
        tenant.setSlug(slug);
        tenant.setPlanName(normalizeUpper(request.planName()));
        return toResponse(tenant);
    }

    @Override
    @Transactional
    public TenantResponse suspend(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        if ("CANCELLED".equals(tenant.getStatus())) {
            throw new BusinessException("No se puede suspender un tenant cancelado");
        }
        tenant.setStatus("SUSPENDED");
        return toResponse(tenant);
    }

    @Override
    @Transactional
    public TenantResponse activate(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        if ("CANCELLED".equals(tenant.getStatus())) {
            throw new BusinessException("No se puede activar un tenant cancelado");
        }
        tenant.setStatus("ACTIVE");
        return toResponse(tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantBusinessInfoResponse getBusinessInfo(UUID tenantId) {
        TenantBusinessInfo bi = businessInfoRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Datos comerciales no encontrados"));
        return toBusinessInfoResponse(bi);
    }

    @Override
    @Transactional
    public TenantBusinessInfoResponse updateBusinessInfo(UUID tenantId, UpdateBusinessInfoRequest request) {
        validateBusinessRule(request.businessType(), request.ruc());
        requireTenant(tenantId);
        TenantBusinessInfo bi = businessInfoRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Datos comerciales no encontrados"));

        String businessType = normalizeUpper(request.businessType());
        String legalName = normalizeNullable(request.legalName());
        String ruc = normalizeNullable(request.ruc());
        boolean documentChanged = !Objects.equals(bi.getBusinessType(), businessType)
            || !Objects.equals(bi.getLegalName(), legalName)
            || !Objects.equals(bi.getRuc(), ruc);

        bi.setBusinessType(businessType);
        bi.setCommercialName(normalizeRequired(request.commercialName()));
        bi.setLegalName(legalName);
        bi.setRuc(ruc);
        bi.setPhone(normalizeNullable(request.phone()));
        bi.setEmail(normalizeEmail(request.email()));
        bi.setAddress(normalizeNullable(request.address()));
        bi.setDistrict(normalizeNullable(request.district()));
        bi.setProvince(normalizeNullable(request.province()));
        bi.setDepartment(normalizeNullable(request.department()));
        if (documentChanged) {
            bi.setDocumentStatus("PENDING_VERIFICATION");
        }

        return toBusinessInfoResponse(bi);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantSettingsResponse getSettings(UUID tenantId) {
        TenantSettings settings = settingsRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Configuracion no encontrada"));
        return toSettingsResponse(settings);
    }

    @Override
    @Transactional
    public TenantSettingsResponse updateSettings(UUID tenantId, UpdateTenantSettingsRequest request) {
        requireTenant(tenantId);
        TenantSettings settings = settingsRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Configuracion no encontrada"));

        if (request.templateId() != null) {
            StoreTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
            if (!template.isActive()) {
                throw new BusinessException("Plantilla no esta activa");
            }
            settings.setTemplate(template);
        }

        settings.setLogoUrl(normalizeNullable(request.logoUrl()));
        settings.setBannerUrl(normalizeNullable(request.bannerUrl()));
        settings.setPrimaryColor(normalizeUpperNullable(request.primaryColor()));
        settings.setSecondaryColor(normalizeUpperNullable(request.secondaryColor()));
        settings.setAccentColor(normalizeUpperNullable(request.accentColor()));
        settings.setFontFamily(normalizeNullable(request.fontFamily()));
        settings.setWhatsappNumber(normalizeNullable(request.whatsappNumber()));
        settings.setInstagramUrl(normalizeNullable(request.instagramUrl()));
        settings.setFacebookUrl(normalizeNullable(request.facebookUrl()));
        settings.setTiktokUrl(normalizeNullable(request.tiktokUrl()));

        return toSettingsResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreInfoResponse getPublicBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);
        Tenant tenant = tenantRepository.findBySlug(normalizedSlug)
            .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada: " + normalizedSlug));
        if (!PUBLIC_STATUSES.contains(tenant.getStatus())) {
            throw new ResourceNotFoundException("Tienda no encontrada: " + normalizedSlug);
        }
        TenantBusinessInfo businessInfo = businessInfoRepository.findByTenantId(tenant.getId()).orElse(null);
        TenantSettings settings = settingsRepository.findByTenantId(tenant.getId()).orElse(null);
        return new StoreInfoResponse(
            tenant.getId(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getStatus(),
            businessInfo != null ? toStoreBusinessInfoResponse(businessInfo) : null,
            settings != null ? toSettingsResponse(settings) : null
        );
    }

    private Tenant requireTenant(UUID id) {
        return tenantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada: " + id));
    }

    private void validateBusinessRule(String businessType, String ruc) {
        String normalizedType = normalizeUpper(businessType);
        String normalizedRuc = normalizeNullable(ruc);
        if (!SUPPORTED_BUSINESS_TYPES.contains(normalizedType)) {
            throw new BusinessException("business_type debe ser EMPRESA o PERSONA_NATURAL");
        }
        if (normalizedRuc != null && !normalizedRuc.matches("^\\d{11}$")) {
            throw new BusinessException("RUC debe tener 11 digitos");
        }
        if ("EMPRESA".equals(normalizedType) && normalizedRuc == null) {
            throw new BusinessException("RUC obligatorio (11 digitos) para business_type EMPRESA");
        }
    }

    private void applyBusinessInfo(TenantBusinessInfo bi, BusinessInfoRequest r) {
        bi.setBusinessType(normalizeUpper(r.businessType()));
        bi.setCommercialName(normalizeRequired(r.commercialName()));
        bi.setLegalName(normalizeNullable(r.legalName()));
        bi.setRuc(normalizeNullable(r.ruc()));
        bi.setPhone(normalizeNullable(r.phone()));
        bi.setEmail(normalizeEmail(r.email()));
        bi.setAddress(normalizeNullable(r.address()));
        bi.setDistrict(normalizeNullable(r.district()));
        bi.setProvince(normalizeNullable(r.province()));
        bi.setDepartment(normalizeNullable(r.department()));
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized != null ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeSlug(String value) {
        return normalizeRequired(value).toLowerCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        return normalizeRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeUpperNullable(String value) {
        String normalized = normalizeNullable(value);
        return normalized != null ? normalized.toUpperCase(Locale.ROOT) : null;
    }

    private TenantResponse toResponse(Tenant t) {
        return new TenantResponse(t.getId(), t.getName(), t.getSlug(), t.getStatus(), t.getPlanName(), t.getCreatedAt());
    }

    private TenantDetailResponse toDetailResponse(Tenant t, TenantBusinessInfo bi, TenantSettings s) {
        return new TenantDetailResponse(
            t.getId(), t.getName(), t.getSlug(), t.getStatus(), t.getPlanName(), t.getCreatedAt(),
            bi != null ? toBusinessInfoResponse(bi) : null,
            s != null ? toSettingsResponse(s) : null
        );
    }

    private TenantBusinessInfoResponse toBusinessInfoResponse(TenantBusinessInfo bi) {
        return new TenantBusinessInfoResponse(
            bi.getBusinessType(), bi.getCommercialName(), bi.getLegalName(), bi.getRuc(),
            bi.getDocumentStatus(), bi.getPhone(), bi.getEmail(), bi.getAddress(),
            bi.getDistrict(), bi.getProvince(), bi.getDepartment()
        );
    }

    private StoreBusinessInfoResponse toStoreBusinessInfoResponse(TenantBusinessInfo bi) {
        return new StoreBusinessInfoResponse(
            bi.getCommercialName(),
            bi.getLegalName(),
            bi.getRuc(),
            bi.getPhone(),
            bi.getEmail(),
            bi.getAddress(),
            bi.getDistrict(),
            bi.getProvince(),
            bi.getDepartment()
        );
    }

    private TenantSettingsResponse toSettingsResponse(TenantSettings s) {
        StoreTemplate t = s.getTemplate();
        return new TenantSettingsResponse(
            t != null ? t.getId() : null,
            t != null ? t.getCode() : null,
            t != null ? t.getName() : null,
            s.getLogoUrl(), s.getBannerUrl(),
            s.getPrimaryColor(), s.getSecondaryColor(), s.getAccentColor(),
            s.getFontFamily(),
            s.getWhatsappNumber(), s.getInstagramUrl(), s.getFacebookUrl(), s.getTiktokUrl()
        );
    }
}
