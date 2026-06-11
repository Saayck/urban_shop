package com.urban_shop.backend.order.mapper;

import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.dto.response.OrderItemResponse;
import com.urban_shop.backend.order.dto.response.OrderResponse;
import com.urban_shop.backend.order.dto.response.OrderStatusHistoryResponse;
import com.urban_shop.backend.order.entity.CustomerOrder;
import com.urban_shop.backend.order.entity.OrderItem;
import com.urban_shop.backend.order.entity.OrderStatusHistory;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(CustomerOrder order, List<OrderItem> items) {
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomerId(),
            order.getTotal(),
            order.getPaymentStatus(),
            order.getOrderStatus(),
            order.getDeliveryType(),
            items.stream().mapToInt(OrderItem::getQuantity).sum(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    public static OrderDetailResponse toDetail(
        CustomerOrder order,
        List<OrderItem> items,
        List<PaymentResponse> payments,
        List<OrderStatusHistory> history
    ) {
        return new OrderDetailResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomerId(),
            order.getSubtotal(),
            order.getShippingCost(),
            order.getDiscountTotal(),
            order.getTotal(),
            order.getPaymentStatus(),
            order.getOrderStatus(),
            order.getDeliveryType(),
            order.getDepartment(),
            order.getProvince(),
            order.getDistrict(),
            order.getAddress(),
            order.getReference(),
            order.getLatitude(),
            order.getLongitude(),
            order.getInvoiceType(),
            order.getDocumentType(),
            order.getDocumentNumber(),
            items.stream().map(OrderMapper::toItem).toList(),
            payments,
            history.stream().map(OrderMapper::toHistory).toList(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private static OrderItemResponse toItem(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getVariantId(),
            item.getProductName(),
            item.getSize(),
            item.getColor(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getTotalPrice()
        );
    }

    private static OrderStatusHistoryResponse toHistory(OrderStatusHistory history) {
        return new OrderStatusHistoryResponse(
            history.getId(),
            history.getStatus(),
            history.getChangedBy(),
            history.getNotes(),
            history.getCreatedAt()
        );
    }
}
