package com.urban_shop.backend.tenant.entity;

import com.urban_shop.backend.common.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenant_business_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantBusinessInfo extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_type", nullable = false, length = 50)
    private String businessType;

    @Column(name = "commercial_name", nullable = false, length = 150)
    private String commercialName;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "document_status", nullable = false, length = 50)
    private String documentStatus = "PENDING_VERIFICATION";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "department", length = 100)
    private String department;
}
