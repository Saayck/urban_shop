package com.urban_shop.backend.tenant.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.tenant.dto.request.BusinessInfoRequest;
import com.urban_shop.backend.tenant.dto.request.CreateTenantRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateBusinessInfoRequest;
import com.urban_shop.backend.tenant.dto.request.UpdateTenantSettingsRequest;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private static final Set<String> PUBLIC_STATUSES = Set.of("ACTIVE", "TRIAL");

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
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Slug ya esta tomado: " + request.slug());
        }
        validateBusinessRule(request.businessInfo().businessType(), request.businessInfo().ruc());
        if (userRepository.existsByEmailIgnoreCase(request.initialAdmin().email())) {
            throw new BusinessException("Email ya esta tomado: " + request.initialAdmin().email());
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        tenant.setPlanName(request.planName());
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
        admin.setFullName(request.initialAdmin().fullName());
        admin.setEmail(request.initialAdmin().email().trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(request.initialAdmin().password()));
        admin.setPhone(request.initialAdmin().phone());
        admin.setActive(true);
        admin.getRoles().add(tenantAdminRole);
        userRepository.save(admin);

        return toDetailResponse(tenant, bi, settings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> list() {
        return tenantRepository.findAll().stream().map(this::toResponse).toList();
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
    public TenantResponse suspend(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
        tenant.setStatus("SUSPENDED");
        return toResponse(tenant);
    }

    @Override
    @Transactional
    public TenantResponse activate(UUID tenantId) {
        Tenant tenant = requireTenant(tenantId);
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
        TenantBusinessInfo bi = businessInfoRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Datos comerciales no encontrados"));

        bi.setBusinessType(request.businessType());
        bi.setCommercialName(request.commercialName());
        bi.setLegalName(request.legalName());
        bi.setRuc(request.ruc());
        bi.setPhone(request.phone());
        bi.setEmail(request.email());
        bi.setAddress(request.address());
        bi.setDistrict(request.district());
        bi.setProvince(request.province());
        bi.setDepartment(request.department());

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

        settings.setLogoUrl(request.logoUrl());
        settings.setBannerUrl(request.bannerUrl());
        settings.setPrimaryColor(request.primaryColor());
        settings.setSecondaryColor(request.secondaryColor());
        settings.setAccentColor(request.accentColor());
        settings.setFontFamily(request.fontFamily());
        settings.setWhatsappNumber(request.whatsappNumber());
        settings.setInstagramUrl(request.instagramUrl());
        settings.setFacebookUrl(request.facebookUrl());
        settings.setTiktokUrl(request.tiktokUrl());

        return toSettingsResponse(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreInfoResponse getPublicBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada: " + slug));
        if (!PUBLIC_STATUSES.contains(tenant.getStatus())) {
            throw new BusinessException("Tienda no disponible");
        }
        TenantSettings settings = settingsRepository.findByTenantId(tenant.getId()).orElse(null);
        return new StoreInfoResponse(
            tenant.getId(),
            tenant.getName(),
            tenant.getSlug(),
            tenant.getStatus(),
            settings != null ? toSettingsResponse(settings) : null
        );
    }

    private Tenant requireTenant(UUID id) {
        return tenantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tienda no encontrada: " + id));
    }

    private void validateBusinessRule(String businessType, String ruc) {
        if ("EMPRESA".equalsIgnoreCase(businessType)) {
            if (ruc == null || !ruc.matches("^\\d{11}$")) {
                throw new BusinessException("RUC obligatorio (11 digitos) para business_type EMPRESA");
            }
        }
    }

    private void applyBusinessInfo(TenantBusinessInfo bi, BusinessInfoRequest r) {
        bi.setBusinessType(r.businessType());
        bi.setCommercialName(r.commercialName());
        bi.setLegalName(r.legalName());
        bi.setRuc(r.ruc());
        bi.setPhone(r.phone());
        bi.setEmail(r.email());
        bi.setAddress(r.address());
        bi.setDistrict(r.district());
        bi.setProvince(r.province());
        bi.setDepartment(r.department());
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
