package com.urban_shop.backend.customer.service;

import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.customer.dto.request.CustomerStatusRequest;
import com.urban_shop.backend.customer.dto.response.CustomerSummaryResponse;
import com.urban_shop.backend.customer.entity.CustomerStatus;
import com.urban_shop.backend.order.dto.response.OrderResponse;

import java.util.UUID;

/**
 * Vista de administracion sobre los clientes de una tienda.
 */
public interface AdminCustomerService {

    PageResponse<CustomerSummaryResponse> list(
        UUID tenantId,
        String search,
        CustomerStatus status,
        int page,
        int size
    );

    CustomerSummaryResponse get(UUID tenantId, UUID customerId);

    PageResponse<OrderResponse> listOrders(UUID tenantId, UUID customerId, int page, int size);

    /** Bloquear o desactivar corta el acceso del cliente y revoca sus sesiones abiertas. */
    CustomerSummaryResponse updateStatus(UUID tenantId, UUID customerId, CustomerStatusRequest request);
}
