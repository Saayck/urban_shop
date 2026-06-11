package com.urban_shop.backend.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRejectRequest(
    @NotBlank @Size(max = 500) String reason
) {
}
