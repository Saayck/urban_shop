package com.urban_shop.backend.payment.entity;

public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELLED,
    REFUNDED,
    MANUAL_REVIEW
}
