package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO: qué tan seguido se usan los descuentos y cuánto representan
 * en dinero. Épica 7: Generación de reportes.
 *
 * Analiza todas las reservas NO canceladas (una reserva cancelada
 * nunca se tradujo en un descuento realmente otorgado).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountEffectivenessResponse {

    private long totalBookingsAnalyzed;
    private long bookingsWithDiscount;
    private long bookingsWithoutDiscount;
    private BigDecimal totalBaseAmount;
    private BigDecimal totalDiscountGiven;
    private List<DiscountTypeUsageResponse> byType;
}
