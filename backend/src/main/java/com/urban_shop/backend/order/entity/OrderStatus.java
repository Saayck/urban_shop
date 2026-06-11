package com.urban_shop.backend.order.entity;

public enum OrderStatus {
    CREATED,
    PAID,
    PREPARING,
    READY_FOR_PICKUP,
    ON_THE_WAY,
    DELIVERED,
    CANCELLED,
    REFUNDED
}
