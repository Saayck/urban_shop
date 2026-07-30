package com.urban_shop.backend.payment.dto.request;

import com.urban_shop.backend.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentWebhookRequest(

    @NotBlank(message = "El proveedor es obligatorio")
    String provider,

    @NotBlank(message = "El identificador de transaccion externo es obligatorio")
    String externalTransactionId,

    @NotNull(message = "El pedido es obligatorio")
    UUID orderId,

    @NotBlank(message = "El estado es obligatorio")
    String status,

    PaymentMethod method,

    String rawPayload
) {
}
