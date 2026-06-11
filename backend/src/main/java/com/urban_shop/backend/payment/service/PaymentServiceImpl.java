package com.urban_shop.backend.payment.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.order.entity.CustomerOrder;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.order.entity.OrderStatusHistory;
import com.urban_shop.backend.order.repository.OrderRepository;
import com.urban_shop.backend.order.repository.OrderStatusHistoryRepository;
import com.urban_shop.backend.payment.dto.request.PaymentCreateRequest;
import com.urban_shop.backend.payment.dto.request.PaymentRejectRequest;
import com.urban_shop.backend.payment.dto.response.PaymentResponse;
import com.urban_shop.backend.payment.entity.Payment;
import com.urban_shop.backend.payment.entity.PaymentMethod;
import com.urban_shop.backend.payment.entity.PaymentStatus;
import com.urban_shop.backend.payment.mapper.PaymentMapper;
import com.urban_shop.backend.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final List<PaymentStatus> OPEN_PAYMENT_STATUSES = List.of(
        PaymentStatus.PENDING,
        PaymentStatus.MANUAL_REVIEW
    );

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;

    @Override
    @Transactional
    public PaymentResponse create(
        UUID tenantId,
        UUID customerId,
        UUID orderId,
        PaymentCreateRequest request
    ) {
        CustomerOrder order = orderRepository.findByTenantIdAndIdForUpdate(tenantId, orderId)
            .filter(candidate -> customerId.equals(candidate.getCustomerId()))
            .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        validatePayable(order);
        if (paymentRepository.existsByTenantIdAndOrderIdAndStatusIn(
            tenantId,
            orderId,
            OPEN_PAYMENT_STATUSES
        )) {
            throw new BusinessException("El pedido ya tiene un pago pendiente de revision");
        }
        validateEvidence(request);

        Payment payment = new Payment();
        payment.setTenantId(tenantId);
        payment.setOrderId(orderId);
        payment.setProvider(provider(request.method()));
        payment.setMethod(request.method());
        payment.setAmount(order.getTotal());
        payment.setOperationCode(trimToNull(request.operationCode()));
        payment.setProofImageUrl(trimToNull(request.proofImageUrl()));
        PaymentStatus status = request.method() == PaymentMethod.CASH_ON_DELIVERY
            ? PaymentStatus.PENDING
            : PaymentStatus.MANUAL_REVIEW;
        payment.setStatus(status);

        order.setPaymentStatus(status == PaymentStatus.PENDING
            ? OrderPaymentStatus.PENDING
            : OrderPaymentStatus.MANUAL_REVIEW);
        orderRepository.save(order);
        return PaymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponse confirm(UUID tenantId, UUID reviewerId, UUID paymentId) {
        Payment reference = paymentRepository.findByTenantIdAndId(tenantId, paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        CustomerOrder order = orderRepository.findByTenantIdAndIdForUpdate(tenantId, reference.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        Payment payment = paymentRepository.findByTenantIdAndIdForUpdate(tenantId, paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        validateReviewable(payment, order);
        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now);
        payment.setReviewedAt(now);
        payment.setReviewedBy(reviewerId);
        payment.setRejectionReason(null);
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.PAID);
        orderRepository.save(order);
        addPaidHistory(tenantId, order.getId(), reviewerId);
        return PaymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponse reject(
        UUID tenantId,
        UUID reviewerId,
        UUID paymentId,
        PaymentRejectRequest request
    ) {
        Payment reference = paymentRepository.findByTenantIdAndId(tenantId, paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        CustomerOrder order = orderRepository.findByTenantIdAndIdForUpdate(tenantId, reference.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        Payment payment = paymentRepository.findByTenantIdAndIdForUpdate(tenantId, paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        validateReviewable(payment, order);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setReviewedAt(LocalDateTime.now());
        payment.setReviewedBy(reviewerId);
        payment.setRejectionReason(request.reason().trim());
        order.setPaymentStatus(OrderPaymentStatus.FAILED);
        orderRepository.save(order);
        return PaymentMapper.toResponse(paymentRepository.save(payment));
    }

    private void validatePayable(CustomerOrder order) {
        if (order.getOrderStatus() == OrderStatus.CANCELLED
            || order.getOrderStatus() == OrderStatus.REFUNDED
            || order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("No se puede pagar un pedido en estado " + order.getOrderStatus());
        }
        if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
            throw new BusinessException("El pedido ya esta pagado");
        }
        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new BusinessException("El pedido no admite nuevos pagos");
        }
    }

    private void validateReviewable(Payment payment, CustomerOrder order) {
        if (!OPEN_PAYMENT_STATUSES.contains(payment.getStatus())) {
            throw new BusinessException("El pago no esta pendiente de revision");
        }
        if (order.getOrderStatus() != OrderStatus.CREATED
            || order.getPaymentStatus() == OrderPaymentStatus.PAID) {
            throw new BusinessException("El pedido no admite revision de pago");
        }
        if (payment.getAmount().compareTo(order.getTotal()) != 0) {
            throw new BusinessException("El monto del pago no coincide con el total del pedido");
        }
    }

    private void validateEvidence(PaymentCreateRequest request) {
        boolean hasCode = request.operationCode() != null && !request.operationCode().isBlank();
        boolean hasProof = request.proofImageUrl() != null && !request.proofImageUrl().isBlank();
        if (request.method() == PaymentMethod.CASH_ON_DELIVERY) {
            if (hasCode || hasProof) {
                throw new BusinessException("Contraentrega no debe incluir comprobante ni codigo");
            }
        } else if (!hasCode && !hasProof) {
            throw new BusinessException("El pago manual requiere codigo de operacion o comprobante");
        }
    }

    private String provider(PaymentMethod method) {
        return switch (method) {
            case YAPE -> "YAPE";
            case PLIN -> "PLIN";
            case BANK_TRANSFER -> "BANK";
            case CASH_ON_DELIVERY -> "CASH";
        };
    }

    private void addPaidHistory(UUID tenantId, UUID orderId, UUID reviewerId) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setTenantId(tenantId);
        history.setOrderId(orderId);
        history.setStatus(OrderStatus.PAID);
        history.setChangedBy(reviewerId);
        history.setNotes("Pago confirmado");
        historyRepository.save(history);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
