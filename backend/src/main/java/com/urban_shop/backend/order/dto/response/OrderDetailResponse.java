package com.urban_shop.backend.order.dto.response;

import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
    UUID id,
    String orderNumber,
    UUID customerId,
    BigDecimal subtotal,
    BigDecimal shippingCost,
    BigDecimal discountTotal,
    BigDecimal total,
    OrderPaymentStatus paymentStatus,
    OrderStatus orderStatus,
    DeliveryType deliveryType,
    String department,
    String province,
    String district,
    String address,
    String reference,
    BigDecimal latitude,
    BigDecimal longitude,
    InvoiceType invoiceType,
    String documentType,
    String documentNumber,
    List<OrderItemResponse> items,
    List<PaymentResponse> payments,
    List<OrderStatusHistoryResponse> tracking,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
