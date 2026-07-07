package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO: reporte de ventas/ingresos en un período.
 * ====================================================================
 * Épica 7: Generación de reportes.
 *
 * Solo cuenta reservas CONFIRMED (efectivamente pagadas) dentro del
 * rango de fechas: una reserva PENDING o EXPIRED no representa dinero
 * realmente recaudado, así que no se contabiliza como "venta".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private long totalConfirmedBookings;
    private BigDecimal totalRevenue;
    private BigDecimal totalDiscountGiven;
    private BigDecimal averageBookingValue;
}
