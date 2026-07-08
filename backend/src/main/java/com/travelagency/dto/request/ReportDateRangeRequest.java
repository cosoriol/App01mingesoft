package com.travelagency.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO: rango de fechas para los reportes administrativos (Épica 7).
 *
 * Representa la forma del pedido de un reporte (fecha de inicio,
 * fecha de término, e "incluir canceladas"). Los endpoints actuales
 * reciben estos mismos datos como @RequestParam directamente (son
 * GET), pero este DTO documenta el contrato del reporte y queda listo
 * para reutilizarse si en el futuro se agrega una variante POST.
 */
@Data
public class ReportDateRangeRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean includeCancelled = false;
}
