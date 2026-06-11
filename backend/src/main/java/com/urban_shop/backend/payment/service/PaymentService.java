package com.urban_shop.backend.payment.service;

import com.urban_shop.backend.payment.dto.request.PaymentCreateRequest;
import com.urban_shop.backend.payment.dto.request.PaymentRejectRequest;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {

    PaymentResponse create(
        UUID tenantId,
        UUID customerId,
        UUID orderId,
        PaymentCreateRequest request
    );

    PaymentResponse confirm(UUID tenantId, UUID reviewerId, UUID paymentId);

    PaymentResponse reject(
        UUID tenantId,
        UUID reviewerId,
        UUID paymentId,
        PaymentRejectRequest request
    );
}
