package com.urban_shop.backend.order.dto.request;

import jakarta.validation.constraints.Size;

public record OrderCancelRequest(
    @Size(max = 500) String reason
) {
}
