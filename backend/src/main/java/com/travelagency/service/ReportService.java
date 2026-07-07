package com.travelagency.service;

import com.travelagency.dto.response.*;
import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SERVICIO DE REPORTES (Capa Service)
 * ======================================
 * Épica 7: Generación de reportes.
 *
 * Genera reportes administrativos a partir de datos que ya existen
 * en el sistema (reservas, paquetes, descuentos). No persiste nada
 * nuevo: solo lee y agrega información ya guardada por las Épicas
 * 2, 4, 5 y 6.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final BookingRepository bookingRepository;
    private final AccessControlService accessControlService;

    /**
     * REPORTE DE VENTAS por período. Solo un ADMIN puede consultarlo.
     * ==================================
     * Solo cuenta reservas CONFIRMED (efectivamente pagadas) dentro
     * del rango de fechas: una reserva PENDING o EXPIRED no
     * representa dinero realmente recaudado.
     *
     * Reutiliza BookingRepository.findBookingsByDateRange (que ya
     * excluye CANCELLED) y filtra a CONFIRMED en memoria, ya que ese
     * método se comparte con otros usos que sí necesitan ver PENDING/EXPIRED.
     *
     * Corrección de bug: antes cualquiera (sin loguearse siquiera)
     * podía ver los ingresos y la actividad del negocio.
     */
    public SalesReportResponse getSalesReport(Long adminUserId, LocalDate startDate, LocalDate endDate) {
        accessControlService.requireAdmin(adminUserId);

        // Regla: el rango de fechas debe ser válido.
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException("Start date must not be after end date");
        }

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(LocalTime.MAX);

        List<Booking> confirmedBookings = bookingRepository.findBookingsByDateRange(rangeStart, rangeEnd)
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();

        BigDecimal totalRevenue = sum(confirmedBookings, Booking::getTotalAmount);
        BigDecimal totalDiscount = sum(confirmedBookings, Booking::getDiscountAmount);
        BigDecimal averageValue = confirmedBookings.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(confirmedBookings.size()), 2, RoundingMode.HALF_UP);

        return new SalesReportResponse(
                startDate, endDate, confirmedBookings.size(), totalRevenue, totalDiscount, averageValue);
    }

    /**
     * REPORTE DE PAQUETES MÁS RESERVADOS.
     * ======================================
     * @param limit cuántos paquetes devolver como máximo (ranking, no paginación)
     */
    public List<PackagePopularityResponse> getMostBookedPackages(Long adminUserId, int limit) {
        accessControlService.requireAdmin(adminUserId);

        if (limit <= 0) {
            throw new BusinessRuleException("Limit must be greater than zero");
        }
        Pageable topN = PageRequest.of(0, limit);
        return bookingRepository.findMostBookedPackages(topN);
    }

    /**
     * REPORTE DE RESUMEN DE RESERVAS POR ESTADO.
     * =============================================
     * Cuántas reservas hay en cada estado (PENDING, CONFIRMED,
     * CANCELLED, EXPIRED), considerando TODAS las reservas del sistema.
     */
    public BookingsSummaryResponse getBookingsSummary(Long adminUserId) {
        accessControlService.requireAdmin(adminUserId);

        List<BookingStatusCountResponse> byStatus = bookingRepository.countBookingsByStatus();
        long total = byStatus.stream().mapToLong(BookingStatusCountResponse::getCount).sum();
        return new BookingsSummaryResponse(total, byStatus);
    }

    /**
     * REPORTE DE EFECTIVIDAD DE DESCUENTOS.
     * ========================================
     * Analiza todas las reservas NO canceladas: cuántas tuvieron
     * algún descuento, cuánto dinero representó en total, y cuántas
     * veces se aplicó cada tipo de descuento (grupo, cliente
     * frecuente, multi-paquete, promoción).
     */
    public DiscountEffectivenessResponse getDiscountEffectiveness(Long adminUserId) {
        accessControlService.requireAdmin(adminUserId);

        List<Booking> bookings = bookingRepository.findByStatusNot(BookingStatus.CANCELLED);

        long withDiscount = bookings.stream()
                .filter(b -> b.getDiscountAmount() != null && b.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal totalBase = sum(bookings, Booking::getBaseAmount);
        BigDecimal totalDiscount = sum(bookings, Booking::getDiscountAmount);

        // Cada reserva guarda su desglose de descuentos como JSON
        // (Épica 4); se reconstruye para contar cuántas veces se usó
        // cada tipo y su porcentaje promedio.
        Map<DiscountType, List<DiscountDetail>> byType = bookings.stream()
                .flatMap(b -> DiscountDetail.fromJson(b.getDiscountDetails()).stream())
                .collect(Collectors.groupingBy(DiscountDetail::getType));

        List<DiscountTypeUsageResponse> breakdown = byType.entrySet().stream()
                .map(entry -> {
                    List<DiscountDetail> items = entry.getValue();
                    BigDecimal averagePercentage = sum(items, DiscountDetail::getPercentage)
                            .divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
                    return new DiscountTypeUsageResponse(entry.getKey(), items.size(), averagePercentage);
                })
                .sorted((a, b) -> Long.compare(b.getTimesApplied(), a.getTimesApplied()))
                .toList();

        return new DiscountEffectivenessResponse(
                bookings.size(), withDiscount, bookings.size() - withDiscount,
                totalBase, totalDiscount, breakdown);
    }

    /** Suma un campo BigDecimal de una lista, tratando null como cero. */
    private <T> BigDecimal sum(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
        return items.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
