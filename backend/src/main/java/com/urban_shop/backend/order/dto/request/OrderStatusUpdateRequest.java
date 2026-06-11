package com.urban_shop.backend.order.dto.request;

import com.urban_shop.backend.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderStatusUpdateRequest(
    @NotNull OrderStatus status,
    @Size(max = 500) String notes
) {
}
