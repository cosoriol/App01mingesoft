package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO: totales del listado detallado de ventas (Épica 7).
 * bookingsByStatus está agrupado por el texto legible del estado
 * (ej. "Confirmada"), listo para mostrarse directamente en el reporte.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportSummary {

    private long totalBookings;
    private long totalPassengers;
    private BigDecimal totalSalesAmount;
    private BigDecimal totalCollectedAmount;
    private Map<String, Long> bookingsByStatus;
}
