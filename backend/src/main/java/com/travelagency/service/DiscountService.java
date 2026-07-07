package com.travelagency.service;

import com.travelagency.config.DiscountConfig;
import com.travelagency.dto.response.DiscountDetail;
import com.travelagency.dto.response.DiscountType;
import com.travelagency.entity.BookingStatus;
import com.travelagency.entity.Promotion;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICIO DE DESCUENTOS (Épica 4: Reglas comerciales de descuentos)
 * =====================================================================
 * Encapsula TODA la lógica de cálculo de descuentos de una reserva,
 * separada de BookingService (principio de responsabilidad única):
 * BookingService se encarga de crear/cancelar/expirar reservas;
 * DiscountService se encarga SOLO de calcular cuánto descuento
 * corresponde y por qué.
 *
 * Ningún umbral ni porcentaje está hardcodeado: todos se leen desde
 * application.properties a través de DiscountConfig, para que se
 * puedan ajustar sin recompilar el proyecto.
 */
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final BookingRepository bookingRepository;
    private final PromotionRepository promotionRepository;
    private final DiscountConfig discountConfig;

    /**
     * Calcula los descuentos aplicables a una reserva y el monto final.
     *
     * Sigue el orden exacto de la Regla 6:
     *   1. baseAmount ya viene calculado por BookingService (precio × pasajeros)
     *   2. Evaluar descuento por grupo
     *   3. Evaluar descuento por cliente frecuente
     *   4. Evaluar descuento por multi-paquete
     *   5. Evaluar promociones vigentes
     *   6. Sumar todos los porcentajes
     *   7. Aplicar tope máximo
     *   8. discountAmount = baseAmount × (totalPercentage / 100)
     *   9. totalAmount = baseAmount - discountAmount (mínimo 0)
     *
     * @param userId         cliente que hace la reserva
     * @param passengerCount cantidad de pasajeros de ESTA reserva
     * @param baseAmount     precio del paquete × cantidad de pasajeros
     */
    public DiscountCalculationResult calculateDiscounts(Long userId, int passengerCount, BigDecimal baseAmount) {

        List<DiscountDetail> breakdown = new ArrayList<>();
        BigDecimal totalPercentage = BigDecimal.ZERO;

        // ---- REGLA 1: descuento por cantidad de pasajeros (grupo) ----
        int groupThreshold = discountConfig.getGroup().getThreshold();
        if (passengerCount >= groupThreshold) {
            BigDecimal percentage = discountConfig.getGroup().getPercentage();
            totalPercentage = totalPercentage.add(percentage);
            breakdown.add(new DiscountDetail(DiscountType.GROUP,
                    "Descuento por grupo (" + groupThreshold + "+ pasajeros)", percentage));
        }

        // ---- REGLA 2: descuento por cliente frecuente ----
        // Es automático: el propio sistema cuenta las reservas CONFIRMED
        // del cliente. El usuario no puede activarlo ni modificarlo.
        long confirmedBookings = bookingRepository.countByUserIdAndStatus(userId, BookingStatus.CONFIRMED);
        int frequentThreshold = discountConfig.getFrequent().getThreshold();
        if (confirmedBookings >= frequentThreshold) {
            BigDecimal percentage = discountConfig.getFrequent().getPercentage();
            totalPercentage = totalPercentage.add(percentage);
            breakdown.add(new DiscountDetail(DiscountType.FREQUENT_CLIENT,
                    "Cliente frecuente (" + confirmedBookings + " reservas confirmadas)", percentage));
        }

        // ---- REGLA 3: descuento por compra de múltiples paquetes ----
        // Si el cliente tiene otra reserva NO cancelada dentro del
        // período configurado (por defecto, los últimos 30 días).
        int multiPackageDays = discountConfig.getMultipackage().getDays();
        LocalDateTime since = LocalDateTime.now().minusDays(multiPackageDays);
        long recentBookings = bookingRepository.countRecentBookingsByUser(userId, since);
        if (recentBookings >= 1) {
            BigDecimal percentage = discountConfig.getMultipackage().getPercentage();
            totalPercentage = totalPercentage.add(percentage);
            breakdown.add(new DiscountDetail(DiscountType.MULTI_PACKAGE,
                    "Compra de múltiples paquetes (otra reserva en los últimos "
                            + multiPackageDays + " días)", percentage));
        }

        // ---- REGLA 5: promociones por tiempo limitado vigentes ----
        // Solo se aplican si la reserva se crea DENTRO del período de
        // vigencia de la promoción (comparación por fecha del día).
        List<Promotion> activePromotions = promotionRepository.findActivePromotions(LocalDateTime.now().toLocalDate());
        for (Promotion promotion : activePromotions) {
            totalPercentage = totalPercentage.add(promotion.getDiscountPercentage());
            breakdown.add(new DiscountDetail(DiscountType.PROMOTION,
                    "Promoción '" + promotion.getName() + "'", promotion.getDiscountPercentage()));
        }

        // ---- REGLA 4: acumulación con tope máximo ----
        // Los descuentos anteriores se SUMAN; si superan el tope
        // configurado, se recorta al tope (nunca se rechaza la reserva
        // por esto, solo se limita el beneficio).
        BigDecimal maxPercentage = discountConfig.getMax().getPercentage();
        if (totalPercentage.compareTo(maxPercentage) > 0) {
            totalPercentage = maxPercentage;
        }

        // ---- REGLA 6 (pasos 8-9): monto de descuento y monto final ----
        BigDecimal discountAmount = baseAmount
                .multiply(totalPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = baseAmount.subtract(discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            // REGLA 4: el monto final NUNCA puede ser negativo.
            totalAmount = BigDecimal.ZERO;
        }

        // ---- REGLA 7: transparencia ----
        // El desglose se serializa a JSON para persistirlo íntegro en
        // Booking.discountDetails. Guardarlo estructurado (en vez de
        // un texto plano armado a mano) permite reconstruirlo tal
        // cual en BookingResponse más adelante, sin volver a calcular
        // los descuentos (que podrían cambiar con el tiempo: el
        // cliente puede dejar de ser "frecuente", una promoción vence).
        String detailsJson = DiscountDetail.toJson(breakdown);

        // Además del JSON, se arma la versión legible para humanos
        // (Booking.discountSummary) usando el tope QUE REALMENTE
        // aplicó en este cálculo, para que quede fijo aunque
        // app.discount.max.percentage cambie más adelante.
        String summary = DiscountDetail.toSummary(breakdown, totalPercentage, maxPercentage);

        return new DiscountCalculationResult(
                totalPercentage, discountAmount, totalAmount, breakdown, detailsJson, summary);
    }
}
