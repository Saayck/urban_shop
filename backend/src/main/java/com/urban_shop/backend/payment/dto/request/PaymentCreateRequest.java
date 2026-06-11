package com.urban_shop.backend.payment.dto.request;

import com.urban_shop.backend.common.validation.HttpUrl;
import com.urban_shop.backend.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentCreateRequest(
    @NotNull PaymentMethod method,
    @Size(max = 100) String operationCode,
    @HttpUrl @Size(max = 2048) String proofImageUrl
) {
}
