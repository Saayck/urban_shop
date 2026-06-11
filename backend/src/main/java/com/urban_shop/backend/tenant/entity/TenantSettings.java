package com.urban_shop.backend.tenant.entity;

import com.urban_shop.backend.common.persistence.TenantScopedEntity;
import com.urban_shop.backend.template.entity.StoreTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenant_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettings extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id")
    private StoreTemplate template;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "primary_color", length = 20)
    private String primaryColor;

    @Column(name = "secondary_color", length = 20)
    private String secondaryColor;

    @Column(name = "accent_color", length = 20)
    private String accentColor;

    @Column(name = "font_family", length = 100)
    private String fontFamily;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "instagram_url", columnDefinition = "TEXT")
    private String instagramUrl;

    @Column(name = "facebook_url", columnDefinition = "TEXT")
    private String facebookUrl;

    @Column(name = "tiktok_url", columnDefinition = "TEXT")
    private String tiktokUrl;
}
