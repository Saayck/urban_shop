package com.urban_shop.backend.customer.dto.request;

import com.urban_shop.backend.customer.entity.CustomerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerStatusRequest(

    @NotNull(message = "El estado es obligatorio")
    CustomerStatus status,

    @Size(max = 500) String reason
) {
}
