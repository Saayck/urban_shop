package com.urban_shop.backend.report.service;

import com.urban_shop.backend.report.dto.LowStockResponse;
import com.urban_shop.backend.report.dto.SalesReportResponse;
import com.urban_shop.backend.report.dto.TopProductResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportService {

    SalesReportResponse getSalesReport(UUID tenantId, LocalDate from, LocalDate to);

    List<TopProductResponse> getTopProducts(UUID tenantId, LocalDate from, LocalDate to, int limit);

    List<LowStockResponse> getLowStock(UUID tenantId, int threshold);

    String generateSalesReportCsv(UUID tenantId, LocalDate from, LocalDate to);
}
