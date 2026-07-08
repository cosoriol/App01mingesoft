package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO: una fila del ranking de paquetes vendidos por período (Épica 7).
 *
 * "rank" se asigna en ReportService.buildRanking DESPUÉS de ordenar
 * por los 4 criterios de desempate (cantidad de reservas, pasajeros,
 * monto total, nombre) — por eso el campo es mutable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageRankingItem {

    private int rank;
    private String packageName;
    private String destination;
    private long bookingCount;
    private long totalPassengers;
    private BigDecimal totalAmount;
    private BigDecimal totalCollected;
    private BigDecimal unitPrice;
}
