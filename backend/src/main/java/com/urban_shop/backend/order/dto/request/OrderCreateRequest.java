package com.urban_shop.backend.order.dto.request;

import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record OrderCreateRequest(
    @NotNull DeliveryType deliveryType,
    UUID addressId,
    InvoiceType invoiceType,
    @Pattern(regexp = "^(DNI|CE|PASSPORT|RUC)$", message = "documentType no valido")
    String documentType,
    @Size(max = 20) String documentNumber
) {
}
