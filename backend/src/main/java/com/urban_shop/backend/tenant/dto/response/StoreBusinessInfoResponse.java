package com.urban_shop.backend.tenant.dto.response;

public record StoreBusinessInfoResponse(
    String commercialName,
    String legalName,
    String ruc,
    String phone,
    String email,
    String address,
    String district,
    String province,
    String department
) {
}
