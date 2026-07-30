package com.urban_shop.backend.auth.entity;

public enum ResetPrincipalType {

    /** Usuario interno: SUPER_ADMIN, TENANT_ADMIN o SALES_STAFF. */
    USER,

    /** Cliente final de una tienda. */
    CUSTOMER
}
