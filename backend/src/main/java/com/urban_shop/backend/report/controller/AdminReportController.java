package com.urban_shop.backend.report.controller;

import com.urban_shop.backend.common.tenant.CurrentTenant;
import com.urban_shop.backend.report.dto.LowStockResponse;
import com.urban_shop.backend.report.dto.SalesReportResponse;
import com.urban_shop.backend.report.dto.TopProductResponse;
import com.urban_shop.backend.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Admin Reports", description = "Reportes y metricas de la tienda")
@RequiredArgsConstructor
@Validated
public class AdminReportController {

    private final ReportService reportService;
    private final CurrentTenant currentTenant;

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(
        summary = "Resumen de ventas",
        description = "Ingresos, pedidos entregados, en curso y cancelados. Sin fechas usa los ultimos 30 dias."
    )
    public SalesReportResponse getSalesReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.getSalesReport(currentTenant.requireId(), from, to);
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(summary = "Productos mas vendidos en el periodo")
    public List<TopProductResponse> getTopProducts(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return reportService.getTopProducts(currentTenant.requireId(), from, to, limit);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(summary = "Variantes activas con stock por debajo del umbral indicado")
    public List<LowStockResponse> getLowStock(
        @RequestParam(defaultValue = "5") @Min(0) @Max(1000) int threshold
    ) {
        return reportService.getLowStock(currentTenant.requireId(), threshold);
    }

    @GetMapping("/sales/csv")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SALES_STAFF')")
    @Operation(summary = "Exportar el detalle de pedidos del periodo en CSV")
    public ResponseEntity<String> exportSalesReportCsv(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String csvContent = reportService.generateSalesReportCsv(currentTenant.requireId(), from, to);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_report.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csvContent);
    }
}
