package com.urban_shop.backend.payment.service;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.payment.dto.request.PaymentCreateRequest;
import com.urban_shop.backend.payment.dto.request.PaymentRejectRequest;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;
import com.urban_shop.backend.payment.entity.PaymentStatus;

import java.util.UUID;

public interface PaymentService {

    PaymentResponse create(
        UUID tenantId,
        UUID customerId,
        UUID orderId,
        PaymentCreateRequest request
    );

    PageResponse<PaymentResponse> listAdmin(
        UUID tenantId,
        PaymentStatus status,
        int page,
        int size
    );

    PaymentResponse getAdmin(UUID tenantId, UUID paymentId);

    PaymentResponse confirm(UUID tenantId, UUID reviewerId, UUID paymentId);

    PaymentResponse reject(
        UUID tenantId,
        UUID reviewerId,
        UUID paymentId,
        PaymentRejectRequest request
    );
}
