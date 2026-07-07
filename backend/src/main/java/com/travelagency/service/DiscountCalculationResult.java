package com.travelagency.service;

import com.travelagency.dto.response.DiscountDetail;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado del cálculo de descuentos para una reserva.
 * ====================================================================
 * Épica 4: Reglas comerciales de descuentos.
 *
 * Objeto interno de la capa Service (no se persiste tal cual): lo
 * produce DiscountService y lo consume BookingService para construir
 * la entidad Booking, sin que BookingService necesite conocer ningún
 * detalle de CÓMO se calcularon los descuentos.
 *
 * @param totalPercentage    porcentaje total ya con el tope máximo aplicado
 * @param discountAmount     monto de descuento en dinero
 * @param totalAmount        monto final a pagar (nunca negativo)
 * @param breakdown          desglose estructurado (un ítem por descuento aplicado)
 * @param discountDetailsJson breakdown ya serializado a JSON, listo para
 *                            persistir en Booking.discountDetails
 * @param discountSummary    versión en texto plano y legible del mismo
 *                            desglose, lista para persistir en
 *                            Booking.discountSummary
 */
public record DiscountCalculationResult(
        BigDecimal totalPercentage,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        List<DiscountDetail> breakdown,
        String discountDetailsJson,
        String discountSummary) {
}
