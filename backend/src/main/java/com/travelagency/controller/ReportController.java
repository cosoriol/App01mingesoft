package com.travelagency.controller;

import com.travelagency.dto.response.*;
import com.travelagency.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * CONTROLADOR DE REPORTES (Capa Controller)
 * ============================================
 * Épica 7: Generación de reportes.
 *
 * Endpoints de solo lectura para administradores: ventas, paquetes
 * más reservados, resumen de reservas por estado, y efectividad de
 * los descuentos aplicados. Todos requieren "userId" del ADMIN que
 * consulta (ver AccessControlService).
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    /**
     * REPORTE DE VENTAS por período.
     * URL: GET /api/reports/sales?startDate=2026-01-01&endDate=2026-12-31&userId=3
     */
    @GetMapping("/sales")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam Long userId) {
        return ResponseEntity.ok(reportService.getSalesReport(userId, startDate, endDate));
    }

    /**
     * REPORTE DE PAQUETES MÁS RESERVADOS.
     * URL: GET /api/reports/packages/most-booked?limit=10&userId=3
     */
    @GetMapping("/packages/most-booked")
    public ResponseEntity<List<PackagePopularityResponse>> getMostBookedPackages(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam Long userId) {
        return ResponseEntity.ok(reportService.getMostBookedPackages(userId, limit));
    }

    /**
     * RESUMEN DE RESERVAS POR ESTADO.
     * URL: GET /api/reports/bookings/summary?userId=3
     */
    @GetMapping("/bookings/summary")
    public ResponseEntity<BookingsSummaryResponse> getBookingsSummary(@RequestParam Long userId) {
        return ResponseEntity.ok(reportService.getBookingsSummary(userId));
    }

    /**
     * EFECTIVIDAD DE DESCUENTOS.
     * URL: GET /api/reports/discounts/effectiveness?userId=3
     */
    @GetMapping("/discounts/effectiveness")
    public ResponseEntity<DiscountEffectivenessResponse> getDiscountEffectiveness(@RequestParam Long userId) {
        return ResponseEntity.ok(reportService.getDiscountEffectiveness(userId));
    }
}
