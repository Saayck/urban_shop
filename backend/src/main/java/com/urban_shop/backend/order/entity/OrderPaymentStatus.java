package com.urban_shop.backend.order.entity;

public enum OrderPaymentStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED,
    MANUAL_REVIEW
}
