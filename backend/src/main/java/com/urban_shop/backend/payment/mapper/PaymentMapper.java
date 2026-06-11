package com.urban_shop.backend.payment.mapper;

import com.urban_shop.backend.payment.dto.response.PaymentResponse;
import com.urban_shop.backend.payment.entity.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getProvider(),
            payment.getMethod(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getOperationCode(),
            payment.getProofImageUrl(),
            payment.getPaidAt(),
            payment.getReviewedBy(),
            payment.getReviewedAt(),
            payment.getRejectionReason(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
