package com.urban_shop.backend.payment.dto.response;

import com.urban_shop.backend.payment.entity.PaymentMethod;
import com.urban_shop.backend.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID orderId,
    String provider,
    PaymentMethod method,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    String operationCode,
    String proofImageUrl,
    LocalDateTime paidAt,
    UUID reviewedBy,
    LocalDateTime reviewedAt,
    String rejectionReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
