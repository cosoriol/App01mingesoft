package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO: una fila del listado detallado de ventas (Épica 7).
 * ====================================================================
 * Representa UNA reserva dentro del reporte de ventas por período.
 *
 * "operationDate" es la fecha de creación de la reserva (la reserva
 * puede haber calificado para el reporte por su fecha de creación o
 * por su fecha de pago — ver ReportService.generateSalesReport — pero
 * la fecha que se muestra como "de la operación" es siempre la de
 * creación; "paymentDate" muestra por separado cuándo se pagó, si
 * corresponde).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesReportItem {

    private LocalDateTime operationDate;
    private String customerName;
    private String customerEmail;
    private String packageName;
    private String destination;
    private Integer passengerCount;
    private BigDecimal baseAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String bookingStatus;
    private LocalDateTime paymentDate;
}
