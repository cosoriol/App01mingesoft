package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO: cuántas veces se aplicó un tipo de descuento (grupo, cliente
 * frecuente, multi-paquete, promoción) y su porcentaje promedio.
 * Épica 7: Generación de reportes ("efectividad de descuentos").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountTypeUsageResponse {

    private DiscountType type;
    private long timesApplied;
    private BigDecimal averagePercentage;
}
