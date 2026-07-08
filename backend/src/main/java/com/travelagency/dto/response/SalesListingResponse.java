package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO: reporte detallado de ventas por período (Épica 7).
 * ====================================================================
 * Distinto de SalesReportResponse (reporte agregado usado por el
 * dashboard de Reports.js): este es el listado fila-por-fila de cada
 * reserva vendida en el período, con su propio resumen de totales.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesListingResponse {

    private List<SalesReportItem> sales;
    private SalesReportSummary summary;
}
