package com.urban_shop.backend.order.service;

import com.urban_shop.backend.audit.service.AuditService;
import com.urban_shop.backend.cart.entity.Cart;
import com.urban_shop.backend.cart.entity.CartItem;
import com.urban_shop.backend.cart.entity.CartStatus;
import com.urban_shop.backend.cart.repository.CartItemRepository;
import com.urban_shop.backend.cart.repository.CartRepository;
import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.coupon.entity.Coupon;
import com.urban_shop.backend.coupon.service.CouponService;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.entity.CustomerAddress;
import com.urban_shop.backend.customer.repository.CustomerAddressRepository;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import com.urban_shop.backend.inventory.dto.request.InventoryMovementRequest;
import com.urban_shop.backend.inventory.entity.InventoryMovementType;
import com.urban_shop.backend.inventory.service.InventoryService;
import com.urban_shop.backend.order.dto.request.OrderCancelRequest;
import com.urban_shop.backend.order.dto.request.OrderCreateRequest;
import com.urban_shop.backend.order.dto.request.OrderStatusUpdateRequest;
import com.urban_shop.backend.order.dto.response.OrderDetailResponse;
import com.urban_shop.backend.order.dto.response.OrderResponse;
import com.urban_shop.backend.order.entity.CustomerOrder;
import com.urban_shop.backend.order.entity.DeliveryType;
import com.urban_shop.backend.order.entity.InvoiceType;
import com.urban_shop.backend.order.entity.OrderItem;
import com.urban_shop.backend.order.entity.OrderPaymentStatus;
import com.urban_shop.backend.order.entity.OrderStatus;
import com.urban_shop.backend.order.entity.OrderStatusHistory;
import com.urban_shop.backend.order.mapper.OrderMapper;
import com.urban_shop.backend.order.repository.OrderItemRepository;
import com.urban_shop.backend.order.repository.OrderRepository;
import com.urban_shop.backend.order.repository.OrderStatusHistoryRepository;
import com.urban_shop.backend.payment.entity.Payment;
import com.urban_shop.backend.payment.entity.PaymentStatus;
import com.urban_shop.backend.payment.mapper.PaymentMapper;
import com.urban_shop.backend.payment.repository.PaymentRepository;
import com.urban_shop.backend.product.entity.Product;
import com.urban_shop.backend.product.entity.ProductStatus;
import com.urban_shop.backend.product.entity.ProductVariant;
import com.urban_shop.backend.email.service.EmailService;
import com.urban_shop.backend.product.repository.ProductRepository;
import com.urban_shop.backend.product.repository.ProductVariantRepository;
import com.urban_shop.backend.shipping.dto.response.ShippingQuoteResponse;
import com.urban_shop.backend.shipping.dto.response.ShippingZoneResponse;
import com.urban_shop.backend.shipping.service.ShippingZoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_NOT_FOUND = "Pedido no encontrado";

    private static final Collection<PaymentStatus> CANCELLABLE_PAYMENT_STATUSES = List.of(
        PaymentStatus.PENDING,
        PaymentStatus.MANUAL_REVIEW
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryService inventoryService;
    private final ShippingZoneService shippingZoneService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final CouponService couponService;

    @Override
    @Transactional
    public OrderDetailResponse create(UUID tenantId, UUID customerId, OrderCreateRequest request) {
        Customer customer = customerRepository.findByTenantIdAndIdForUpdate(tenantId, customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        if (!customer.isActive()) {
            throw new BusinessException("El cliente no esta activo");
        }

        Cart cart = cartRepository.findByTenantIdAndCustomerIdAndStatusForUpdate(
                tenantId,
                customerId,
                CartStatus.ACTIVE
            )
            .orElseThrow(() -> new BusinessException("El carrito esta vacio"));
        List<CartItem> cartItems = new ArrayList<>(
            cartItemRepository.findAllByCartIdOrderByCreatedAtAsc(cart.getId())
        );
        if (cartItems.isEmpty()) {
            throw new BusinessException("El pedido debe tener al menos un producto");
        }
        cartItems.sort(Comparator.comparing(item -> item.getVariantId().toString()));

        CustomerAddress address = validateDelivery(customerId, request);
        DocumentData document = validateDocument(request);
        CustomerOrder order = createOrder(tenantId, customerId, request, address, document);
        order.setShippingCost(resolveShippingCost(tenantId, request.deliveryType(), address));
        order = orderRepository.save(order);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            ProductVariant variant = variantRepository
                .findByTenantIdAndIdForUpdate(tenantId, cartItem.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));
            Product product = productRepository.findByTenantIdAndId(tenantId, cartItem.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            validatePurchasable(cartItem, product, variant);

            BigDecimal unitPrice = currentPrice(product, variant);
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(totalPrice);
            orderItemRepository.save(toOrderItem(tenantId, order.getId(), cartItem, product, variant, unitPrice));
            inventoryService.create(
                tenantId,
                new InventoryMovementRequest(
                    variant.getId(),
                    InventoryMovementType.SALE,
                    cartItem.getQuantity(),
                    "Pedido " + order.getOrderNumber()
                )
            );
        }

        order.setSubtotal(subtotal);
        applyCoupon(tenantId, customerId, order, subtotal, request.couponCode());
        order.setTotal(subtotal.add(order.getShippingCost()).subtract(order.getDiscountTotal()));
        orderRepository.save(order);
        cart.setStatus(CartStatus.ORDERED);
        cartRepository.save(cart);
        addHistory(tenantId, order.getId(), OrderStatus.CREATED, null, "Pedido creado");
        emailService.sendOrderConfirmationEmail(
            customer.getEmail(),
            order.getOrderNumber(),
            order.getTotal().toPlainString()
        );
        return buildDetail(order);
    }

    /**
     * Valida y canjea el cupon sobre el subtotal ya calculado. El descuento se aplica al
     * subtotal, nunca al costo de envio, y el canje ocurre dentro de la misma transaccion
     * del pedido para que un fallo posterior no consuma el cupon.
     */
    private void applyCoupon(
        UUID tenantId,
        UUID customerId,
        CustomerOrder order,
        BigDecimal subtotal,
        String couponCode
    ) {
        if (couponCode == null || couponCode.isBlank()) {
            order.setDiscountTotal(BigDecimal.ZERO);
            return;
        }

        Coupon coupon = couponService.validateForCheckout(tenantId, customerId, couponCode, subtotal);
        BigDecimal discount = couponService.calculateDiscount(coupon, subtotal);

        order.setDiscountTotal(discount);
        order.setCouponId(coupon.getId());
        order.setCouponCode(coupon.getCode());
        couponService.redeem(tenantId, customerId, order.getId(), coupon, discount);
    }

    /**
     * Aplica la tarifa de la zona de envio que cubre la direccion. Si la tienda tiene zonas
     * configuradas pero ninguna cubre la direccion, el pedido se rechaza en vez de despachar gratis.
     */
    private BigDecimal resolveShippingCost(UUID tenantId, DeliveryType deliveryType, CustomerAddress address) {
        if (deliveryType != DeliveryType.DELIVERY || address == null) {
            return BigDecimal.ZERO;
        }
        ShippingQuoteResponse quote = shippingZoneService.quote(
            tenantId,
            address.getDepartment(),
            address.getProvince(),
            address.getDistrict()
        );
        if (quote.covered()) {
            return quote.price();
        }
        boolean shippingConfigured = shippingZoneService.listAdmin(tenantId).stream()
            .anyMatch(ShippingZoneResponse::active);
        if (!shippingConfigured) {
            // La tienda no usa zonas de envio: no se cobra despacho.
            return BigDecimal.ZERO;
        }
        throw new BusinessException("La tienda no realiza envios a la direccion seleccionada");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listCustomer(UUID tenantId, UUID customerId, int page, int size) {
        Page<CustomerOrder> orders = orderRepository.findAllByTenantIdAndCustomerId(
            tenantId,
            customerId,
            pageRequest(page, size)
        );
        return mapPage(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getCustomer(UUID tenantId, UUID customerId, UUID orderId) {
        CustomerOrder order = orderRepository.findByTenantIdAndCustomerIdAndId(
                tenantId,
                customerId,
                orderId
            )
            .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        return buildDetail(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listAdmin(
        UUID tenantId,
        OrderStatus orderStatus,
        OrderPaymentStatus paymentStatus,
        int page,
        int size
    ) {
        return mapPage(orderRepository.findAdmin(
            tenantId,
            orderStatus,
            paymentStatus,
            pageRequest(page, size)
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getAdmin(UUID tenantId, UUID orderId) {
        CustomerOrder order = orderRepository.findByTenantIdAndId(tenantId, orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        return buildDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse updateStatus(
        UUID tenantId,
        UUID changedBy,
        UUID orderId,
        OrderStatusUpdateRequest request
    ) {
        CustomerOrder order = requireOrderForUpdate(tenantId, orderId);
        OrderStatus target = request.status();
        OrderStatus current = order.getOrderStatus();
        if (order.getOrderStatus() == target) {
            return buildDetail(order);
        }

        if (target == OrderStatus.CANCELLED) {
            cancelCreatedOrder(order, changedBy, request.notes());
            return buildDetail(order);
        }

        validateTransition(order, target);
        order.setOrderStatus(target);
        orderRepository.save(order);
        addHistory(tenantId, orderId, target, changedBy, request.notes());
        auditService.record("ORDER_STATUS_CHANGED", "CustomerOrder", orderId, current.name(), target.name());
        notifyStatusChange(order, target);
        return buildDetail(order);
    }

    @Override
    @Transactional
    public OrderDetailResponse cancelByCustomer(
        UUID tenantId,
        UUID customerId,
        UUID orderId,
        OrderCancelRequest request
    ) {
        CustomerOrder order = requireOrderForUpdate(tenantId, orderId);
        if (!customerId.equals(order.getCustomerId())) {
            throw new ResourceNotFoundException(ORDER_NOT_FOUND);
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return buildDetail(order);
        }

        String notes = request != null && request.reason() != null && !request.reason().isBlank()
            ? "Cancelado por el cliente: " + request.reason().trim()
            : "Cancelado por el cliente";
        // changedBy referencia a users(id): en una cancelacion del cliente no hay usuario interno.
        cancelCreatedOrder(order, null, notes);
        auditService.record("ORDER_CANCELLED_BY_CUSTOMER", "CustomerOrder", orderId, "CREATED", notes);
        notifyStatusChange(order, OrderStatus.CANCELLED);
        return buildDetail(order);
    }

    private void notifyStatusChange(CustomerOrder order, OrderStatus status) {
        customerRepository.findById(order.getCustomerId()).ifPresent(customer ->
            emailService.sendOrderStatusEmail(customer.getEmail(), order.getOrderNumber(), status.name())
        );
    }

    private void validateTransition(CustomerOrder order, OrderStatus target) {
        OrderStatus current = order.getOrderStatus();
        boolean allowed = switch (current) {
            case PAID -> target == OrderStatus.PREPARING;
            case PREPARING -> order.getDeliveryType() == DeliveryType.DELIVERY
                ? target == OrderStatus.ON_THE_WAY
                : target == OrderStatus.READY_FOR_PICKUP;
            case ON_THE_WAY, READY_FOR_PICKUP -> target == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException("Transicion de pedido no permitida: " + current + " -> " + target);
        }
        if (target == OrderStatus.DELIVERED && order.getPaymentStatus() != OrderPaymentStatus.PAID) {
            throw new BusinessException("No se puede entregar un pedido no pagado");
        }
    }

    private void cancelCreatedOrder(CustomerOrder order, UUID changedBy, String notes) {
        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new BusinessException("Solo se puede cancelar un pedido en estado CREATED");
        }
        if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
            throw new BusinessException("Un pedido pagado requiere un proceso de reembolso");
        }

        for (OrderItem item : orderItemRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId())) {
            inventoryService.create(
                order.getTenantId(),
                new InventoryMovementRequest(
                    item.getVariantId(),
                    InventoryMovementType.RETURN,
                    item.getQuantity(),
                    "Cancelacion de pedido " + order.getOrderNumber()
                )
            );
        }
        List<Payment> payments = paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId());
        payments.stream()
            .filter(payment -> CANCELLABLE_PAYMENT_STATUSES.contains(payment.getStatus()))
            .forEach(payment -> payment.setStatus(PaymentStatus.CANCELLED));
        paymentRepository.saveAll(payments);

        // El cupon vuelve al inventario: el pedido que lo consumio ya no existe.
        couponService.releaseForOrder(order.getTenantId(), order.getId());
        order.setCouponId(null);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(OrderPaymentStatus.CANCELLED);
        orderRepository.save(order);
        addHistory(order.getTenantId(), order.getId(), OrderStatus.CANCELLED, changedBy, notes);
    }

    private CustomerOrder createOrder(
        UUID tenantId,
        UUID customerId,
        OrderCreateRequest request,
        CustomerAddress address,
        DocumentData document
    ) {
        CustomerOrder order = new CustomerOrder();
        order.setTenantId(tenantId);
        order.setCustomerId(customerId);
        order.setOrderNumber(generateOrderNumber());
        order.setSubtotal(BigDecimal.ZERO);
        order.setShippingCost(BigDecimal.ZERO);
        order.setDiscountTotal(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);
        order.setDeliveryType(request.deliveryType());
        order.setInvoiceType(document.invoiceType());
        order.setDocumentType(document.type());
        order.setDocumentNumber(document.number());
        if (address != null) {
            order.setDepartment(address.getDepartment());
            order.setProvince(address.getProvince());
            order.setDistrict(address.getDistrict());
            order.setAddress(address.getAddress());
            order.setReference(address.getReference());
            order.setLatitude(address.getLatitude());
            order.setLongitude(address.getLongitude());
        }
        return order;
    }

    private CustomerAddress validateDelivery(UUID customerId, OrderCreateRequest request) {
        if (request.deliveryType() == DeliveryType.DELIVERY) {
            if (request.addressId() == null) {
                throw new BusinessException("La direccion es obligatoria para delivery");
            }
            return addressRepository.findByIdAndCustomerId(request.addressId(), customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Direccion no encontrada"));
        }
        if (request.addressId() != null) {
            throw new BusinessException("STORE_PICKUP no debe incluir direccion");
        }
        return null;
    }

    private DocumentData validateDocument(OrderCreateRequest request) {
        InvoiceType invoiceType = request.invoiceType() == null ? InvoiceType.BOLETA : request.invoiceType();
        String type = trimToNull(request.documentType());
        String number = trimToNull(request.documentNumber());
        if ((type == null) != (number == null)) {
            throw new BusinessException("Tipo y numero de documento deben enviarse juntos");
        }
        if (invoiceType == InvoiceType.FACTURA) {
            if (!"RUC".equals(type) || number == null || !number.matches("^\\d{11}$")) {
                throw new BusinessException("FACTURA requiere RUC de 11 digitos");
            }
        } else if (type != null && !validPersonalDocument(type, number)) {
            throw new BusinessException("Documento no valido para boleta");
        }
        return new DocumentData(invoiceType, type, number == null ? null : number.toUpperCase(Locale.ROOT));
    }

    private boolean validPersonalDocument(String type, String number) {
        return switch (type) {
            case "DNI" -> number.matches("^\\d{8}$");
            case "CE" -> number.matches("^\\d{9,12}$");
            case "PASSPORT" -> number.matches("^[A-Za-z0-9]{6,12}$");
            default -> false;
        };
    }

    private void validatePurchasable(CartItem cartItem, Product product, ProductVariant variant) {
        if (!variant.getProductId().equals(product.getId())
            || !cartItem.getProductId().equals(product.getId())) {
            throw new BusinessException("La variante no pertenece al producto del carrito");
        }
        if (product.getStatus() != ProductStatus.ACTIVE || !variant.isActive()) {
            throw new BusinessException("El producto ya no esta disponible");
        }
        if (variant.getStock() < cartItem.getQuantity()) {
            throw new BusinessException(
                "Stock insuficiente para " + product.getName() + ". Disponible: " + variant.getStock()
            );
        }
    }

    private OrderItem toOrderItem(
        UUID tenantId,
        UUID orderId,
        CartItem cartItem,
        Product product,
        ProductVariant variant,
        BigDecimal unitPrice
    ) {
        OrderItem item = new OrderItem();
        item.setTenantId(tenantId);
        item.setOrderId(orderId);
        item.setProductId(product.getId());
        item.setVariantId(variant.getId());
        item.setProductName(product.getName());
        item.setSize(variant.getSize());
        item.setColor(variant.getColor());
        item.setQuantity(cartItem.getQuantity());
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        return item;
    }

    private BigDecimal currentPrice(Product product, ProductVariant variant) {
        if (variant.getPrice() != null) {
            return variant.getPrice();
        }
        return product.getSalePrice() == null ? product.getBasePrice() : product.getSalePrice();
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<OrderResponse> mapPage(Page<CustomerOrder> orders) {
        if (orders.isEmpty()) {
            return new PageResponse<>(
                List.of(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages()
            );
        }
        List<UUID> orderIds = orders.getContent().stream().map(CustomerOrder::getId).toList();
        Map<UUID, List<OrderItem>> items = orderItemRepository.findAllByOrderIdIn(orderIds).stream()
            .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return PageResponse.from(orders.map(
            order -> OrderMapper.toResponse(order, items.getOrDefault(order.getId(), List.of()))
        ));
    }

    private OrderDetailResponse buildDetail(CustomerOrder order) {
        return OrderMapper.toDetail(
            order,
            orderItemRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId()),
            paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(PaymentMapper::toResponse)
                .toList(),
            historyRepository.findAllByOrderIdOrderByCreatedAtAsc(order.getId())
        );
    }

    private CustomerOrder requireOrderForUpdate(UUID tenantId, UUID orderId) {
        return orderRepository.findByTenantIdAndIdForUpdate(tenantId, orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
    }

    private void addHistory(
        UUID tenantId,
        UUID orderId,
        OrderStatus status,
        UUID changedBy,
        String notes
    ) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setTenantId(tenantId);
        history.setOrderId(orderId);
        history.setStatus(status);
        history.setChangedBy(changedBy);
        history.setNotes(trimToNull(notes));
        historyRepository.save(history);
    }

    private String generateOrderNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).toString().replace("-", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return "ORD-" + date + "-" + suffix;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DocumentData(InvoiceType invoiceType, String type, String number) {
    }
}
