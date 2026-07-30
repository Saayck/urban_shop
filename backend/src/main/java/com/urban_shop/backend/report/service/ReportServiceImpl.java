package com.urban_shop.backend.report.service;

import com.urban_shop.backend.common.exception.BusinessException;
import com.urban_shop.backend.order.entity.CustomerOrder;
import com.urban_shop.backend.order.repository.OrderItemRepository;
import com.urban_shop.backend.order.repository.OrderRepository;
import com.urban_shop.backend.product.repository.ProductVariantRepository;
import com.urban_shop.backend.report.dto.LowStockResponse;
import com.urban_shop.backend.report.dto.SalesReportResponse;
import com.urban_shop.backend.report.dto.SalesSummary;
import com.urban_shop.backend.report.dto.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Reportes de venta. La agregacion ocurre en base de datos: cargar todos los pedidos
 * del tenant en memoria no escala.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MAX_RANGE_DAYS = 366;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;

    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(UUID tenantId, LocalDate from, LocalDate to) {
        DateRange range = resolveRange(from, to);
        SalesSummary summary = orderRepository.summarize(tenantId, range.fromDateTime(), range.toDateTime());
        BigDecimal totalRevenue = summary.totalRevenue() == null ? BigDecimal.ZERO : summary.totalRevenue();

        long billableOrders = summary.totalOrders() - summary.cancelledOrders();
        BigDecimal averageTicket = billableOrders == 0
            ? BigDecimal.ZERO
            : totalRevenue.divide(BigDecimal.valueOf(billableOrders), 2, RoundingMode.HALF_UP);

        return new SalesReportResponse(
            range.from(),
            range.to(),
            summary.totalOrders(),
            summary.completedOrders(),
            summary.pendingOrders(),
            summary.cancelledOrders(),
            totalRevenue,
            averageTicket
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(UUID tenantId, LocalDate from, LocalDate to, int limit) {
        DateRange range = resolveRange(from, to);
        return orderItemRepository.findTopProducts(
            tenantId,
            range.fromDateTime(),
            range.toDateTime(),
            PageRequest.of(0, limit)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LowStockResponse> getLowStock(UUID tenantId, int threshold) {
        return variantRepository.findLowStock(tenantId, threshold);
    }

    @Override
    @Transactional(readOnly = true)
    public String generateSalesReportCsv(UUID tenantId, LocalDate from, LocalDate to) {
        DateRange range = resolveRange(from, to);
        List<CustomerOrder> orders = orderRepository.findForExport(
            tenantId,
            range.fromDateTime(),
            range.toDateTime()
        );

        StringBuilder csv = new StringBuilder(
            "Order ID,Order Number,Order Status,Payment Status,Subtotal,Shipping,Total,Created At\n"
        );
        for (CustomerOrder order : orders) {
            csv.append(order.getId()).append(',')
                .append(csvValue(order.getOrderNumber())).append(',')
                .append(order.getOrderStatus()).append(',')
                .append(order.getPaymentStatus()).append(',')
                .append(order.getSubtotal()).append(',')
                .append(order.getShippingCost()).append(',')
                .append(order.getTotal()).append(',')
                .append(order.getCreatedAt()).append('\n');
        }
        return csv.toString();
    }

    private DateRange resolveRange(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS);
        if (start.isAfter(end)) {
            throw new BusinessException("La fecha inicial no puede ser posterior a la final");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new BusinessException("El rango del reporte no puede superar " + MAX_RANGE_DAYS + " dias");
        }
        return new DateRange(start, end);
    }

    /** Escapa comas y comillas para que el CSV no se rompa con valores que las contengan. */
    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private record DateRange(LocalDate from, LocalDate to) {

        LocalDateTime fromDateTime() {
            return from.atStartOfDay();
        }

        /** Exclusivo: incluye el dia completo de {@code to}. */
        LocalDateTime toDateTime() {
            return to.plusDays(1).atTime(LocalTime.MIDNIGHT);
        }
    }
}
