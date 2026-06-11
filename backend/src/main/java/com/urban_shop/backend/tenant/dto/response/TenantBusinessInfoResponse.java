package com.urban_shop.backend.tenant.dto.response;

public record TenantBusinessInfoResponse(
    String businessType,
    String commercialName,
    String legalName,
    String ruc,
    String documentStatus,
    String phone,
    String email,
    String address,
    String district,
    String province,
    String department
) {
}
