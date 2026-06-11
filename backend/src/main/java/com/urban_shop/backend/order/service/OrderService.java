package com.urban_shop.backend.order.service;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.dto.request.OrderStatusUpdateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.dto.response.OrderResponse;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;

import java.util.UUID;

public interface OrderService {

    OrderDetailResponse create(UUID tenantId, UUID customerId, OrderCreateRequest request);

    PageResponse<OrderResponse> listCustomer(UUID tenantId, UUID customerId, int page, int size);

    OrderDetailResponse getCustomer(UUID tenantId, UUID customerId, UUID orderId);

    PageResponse<OrderResponse> listAdmin(
        UUID tenantId,
        OrderStatus orderStatus,
        OrderPaymentStatus paymentStatus,
        int page,
        int size
    );

    OrderDetailResponse getAdmin(UUID tenantId, UUID orderId);

    OrderDetailResponse updateStatus(
        UUID tenantId,
        UUID changedBy,
        UUID orderId,
        OrderStatusUpdateRequest request
    );
}
