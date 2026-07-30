package com.urban_shop.backend.customer.service;

import com.urban_shop.backend.audit.service.AuditService;
import com.urban_shop.backend.auth.repository.RefreshTokenRepository;
import com.urban_shop.backend.common.exception.ResourceNotFoundException;
import com.urban_shop.backend.common.response.PageResponse;
import com.urban_shop.backend.customer.dto.request.CustomerStatusRequest;
import com.urban_shop.backend.customer.dto.response.CustomerSummaryResponse;
import com.urban_shop.backend.customer.entity.Customer;
import com.urban_shop.backend.customer.entity.CustomerStatus;
import com.urban_shop.backend.customer.repository.CustomerRepository;
import com.urban_shop.backend.order.dto.response.OrderResponse;
import com.urban_shop.backend.order.repository.CustomerPurchaseStats;
import com.urban_shop.backend.order.repository.OrderRepository;
import com.urban_shop.backend.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private static final String CUSTOMER_NOT_FOUND = "Cliente no encontrado";

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerSummaryResponse> list(
        UUID tenantId,
        String search,
        CustomerStatus status,
        int page,
        int size
    ) {
        Page<Customer> customers = customerRepository.searchAdmin(
            tenantId,
            blankToNull(search),
            status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        if (customers.isEmpty()) {
            return PageResponse.from(customers.map(customer -> toSummary(customer, null)));
        }

        // Una sola consulta agregada para toda la pagina, en vez de una por cliente.
        List<UUID> customerIds = customers.getContent().stream().map(Customer::getId).toList();
        Map<UUID, CustomerPurchaseStats> stats = orderRepository
            .findPurchaseStats(tenantId, customerIds).stream()
            .collect(Collectors.toMap(CustomerPurchaseStats::customerId, Function.identity()));

        return PageResponse.from(customers.map(customer -> toSummary(customer, stats.get(customer.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerSummaryResponse get(UUID tenantId, UUID customerId) {
        Customer customer = requireCustomer(tenantId, customerId);
        CustomerPurchaseStats stats = orderRepository
            .findPurchaseStats(tenantId, List.of(customerId)).stream()
            .findFirst()
            .orElse(null);
        return toSummary(customer, stats);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listOrders(UUID tenantId, UUID customerId, int page, int size) {
        requireCustomer(tenantId, customerId);
        return orderService.listCustomer(tenantId, customerId, page, size);
    }

    @Override
    @Transactional
    public CustomerSummaryResponse updateStatus(
        UUID tenantId,
        UUID customerId,
        CustomerStatusRequest request
    ) {
        Customer customer = requireCustomer(tenantId, customerId);
        CustomerStatus previous = customer.getStatus();
        if (previous == request.status()) {
            return get(tenantId, customerId);
        }

        customer.setStatus(request.status());
        customerRepository.save(customer);

        if (request.status() != CustomerStatus.ACTIVE) {
            // Sin esto el cliente seguiria navegando con el token que ya tenia emitido.
            refreshTokenRepository.revokeAllForPrincipal(customerId, LocalDateTime.now());
        }
        auditService.record(
            "CUSTOMER_STATUS_CHANGED",
            "Customer",
            customerId,
            previous.name(),
            request.status().name() + (request.reason() == null ? "" : ": " + request.reason().trim())
        );
        log.info("SECURITY AUDIT: cliente {} paso de {} a {}", customer.getEmail(), previous, request.status());
        return get(tenantId, customerId);
    }

    private Customer requireCustomer(UUID tenantId, UUID customerId) {
        return customerRepository.findByTenantIdAndId(tenantId, customerId)
            .orElseThrow(() -> new ResourceNotFoundException(CUSTOMER_NOT_FOUND));
    }

    private CustomerSummaryResponse toSummary(Customer customer, CustomerPurchaseStats stats) {
        return new CustomerSummaryResponse(
            customer.getId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getDocumentType(),
            customer.getDocumentNumber(),
            customer.isEmailVerified(),
            customer.getStatus(),
            stats == null ? 0L : stats.totalOrders(),
            stats == null || stats.totalSpent() == null ? BigDecimal.ZERO : stats.totalSpent(),
            stats == null ? null : stats.lastOrderAt(),
            customer.getCreatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
