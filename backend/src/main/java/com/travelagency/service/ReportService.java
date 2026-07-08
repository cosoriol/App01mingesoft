package com.travelagency.service;

import com.travelagency.dto.response.*;
import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import com.travelagency.entity.Payment;
import com.travelagency.entity.TravelPackage;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PaymentRepository;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final PaymentRepository paymentRepository;
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

    /**
     * REPORTE DETALLADO DE VENTAS POR PERÍODO (listado fila por fila).
     * ====================================================================
     * Distinto de getSalesReport (agregado, solo CONFIRMED): este
     * reporte lista CADA reserva individual del período, para que el
     * admin pueda auditar/exportar el detalle completo.
     *
     * Regla: se incluye una reserva si su fecha de CREACIÓN o su fecha
     * de PAGO cae dentro del rango (una reserva creada antes del
     * período pero pagada durante él también debe aparecer). Por
     * defecto se excluyen CANCELLED y EXPIRED; includeCancelled=true
     * las incluye a todas.
     *
     * @param adminUserId      quien pide el reporte (debe ser ADMIN)
     * @param startDate        inicio del período (inclusive)
     * @param endDate          fin del período (inclusive)
     * @param includeCancelled si es true, incluye también CANCELLED y EXPIRED
     */
    public SalesListingResponse generateSalesReport(
            Long adminUserId, LocalDate startDate, LocalDate endDate, boolean includeCancelled) {
        accessControlService.requireAdmin(adminUserId);
        validateDateRange(startDate, endDate);

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(LocalTime.MAX);
        List<BookingStatus> excluded = List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED);

        // 1) Reservas cuya fecha de CREACIÓN cae en el rango.
        List<Booking> byCreationDate = includeCancelled
                ? bookingRepository.findByCreatedAtBetween(rangeStart, rangeEnd)
                : bookingRepository.findByCreatedAtBetweenAndStatusNotIn(rangeStart, rangeEnd, excluded);

        // 2) Reservas cuya fecha de PAGO cae en el rango, aunque su
        // creación haya sido antes del período (Regla: "creación O pago").
        List<Booking> byPaymentDate = paymentRepository.findByPaymentDateBetween(rangeStart, rangeEnd).stream()
                .map(Payment::getBooking)
                .filter(b -> includeCancelled || !excluded.contains(b.getStatus()))
                .toList();

        // 3) Unir ambos conjuntos sin duplicar reservas (por id), y
        // ordenar por fecha de creación descendente (más reciente primero).
        Map<Long, Booking> merged = new LinkedHashMap<>();
        byCreationDate.forEach(b -> merged.put(b.getId(), b));
        byPaymentDate.forEach(b -> merged.putIfAbsent(b.getId(), b));

        List<Booking> bookings = merged.values().stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .toList();

        if (bookings.isEmpty()) {
            return new SalesListingResponse(List.of(), buildSalesSummary(List.of()));
        }

        // Traer los pagos de todas estas reservas de una sola vez (evita N+1).
        Map<Long, Payment> paymentsByBookingId = paymentsByBookingId(bookings);

        List<SalesReportItem> items = bookings.stream()
                .map(b -> mapToSalesItem(b, paymentsByBookingId.get(b.getId())))
                .toList();

        return new SalesListingResponse(items, buildSalesSummary(items));
    }

    /** Convierte una reserva (y su pago, si existe) en una fila del reporte de ventas. */
    private SalesReportItem mapToSalesItem(Booking booking, Payment payment) {
        return new SalesReportItem(
                booking.getCreatedAt(),
                booking.getUser().getFullName(),
                booking.getUser().getEmail(),
                booking.getTravelPackage().getName(),
                booking.getTravelPackage().getDestination(),
                booking.getPassengerCount(),
                booking.getBaseAmount(),
                booking.getDiscountPercentage(),
                booking.getDiscountAmount(),
                booking.getTotalAmount(),
                payment != null ? payment.getAmount() : BigDecimal.ZERO,
                toReadableStatus(booking.getStatus()),
                payment != null ? payment.getPaymentDate() : null);
    }

    /** Calcula los totales del reporte de ventas a partir de sus filas. */
    private SalesReportSummary buildSalesSummary(List<SalesReportItem> items) {
        long totalPassengers = items.stream().mapToLong(SalesReportItem::getPassengerCount).sum();
        BigDecimal totalSales = sum(items, SalesReportItem::getTotalAmount);
        BigDecimal totalCollected = sum(items, SalesReportItem::getPaidAmount);
        Map<String, Long> byStatus = items.stream()
                .collect(Collectors.groupingBy(SalesReportItem::getBookingStatus, Collectors.counting()));
        return new SalesReportSummary(items.size(), totalPassengers, totalSales, totalCollected, byStatus);
    }

    /** Traduce el estado interno de una reserva a texto legible para el reporte. */
    private String toReadableStatus(BookingStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente de pago";
            case CONFIRMED -> "Confirmada";
            case CANCELLED -> "Cancelada";
            case EXPIRED -> "Expirada";
        };
    }

    /**
     * RANKING DE PAQUETES VENDIDOS POR PERÍODO.
     * ====================================================================
     * Agrupa las reservas del período (excluyendo siempre CANCELLED y
     * EXPIRED) por paquete turístico, y las ordena de mayor a menor
     * demanda con 4 niveles de desempate (ver buildRanking).
     */
    public PackageRankingResponse generatePackageRanking(Long adminUserId, LocalDate startDate, LocalDate endDate) {
        accessControlService.requireAdmin(adminUserId);
        validateDateRange(startDate, endDate);

        LocalDateTime rangeStart = startDate.atStartOfDay();
        LocalDateTime rangeEnd = endDate.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findByCreatedAtBetweenAndStatusNotIn(
                rangeStart, rangeEnd, List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED));

        List<PackageRankingItem> ranking = buildRanking(bookings);
        return new PackageRankingResponse(ranking, buildRankingSummary(ranking));
    }

    /**
     * Agrupa las reservas por paquete y arma el ranking ordenado.
     *
     * Criterio de orden (en cascada, cada uno desempata al anterior):
     * 1. Cantidad de reservas (mayor a menor)
     * 2. Número total de pasajeros (mayor a menor)
     * 3. Monto total generado (mayor a menor)
     * 4. Nombre del paquete (alfabético)
     */
    private List<PackageRankingItem> buildRanking(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }

        Map<Long, Payment> paymentsByBookingId = paymentsByBookingId(bookings);

        // Se agrupa por el ID del paquete (no por la entidad completa)
        // para no depender de equals/hashCode de TravelPackage.
        Map<Long, List<Booking>> byPackageId = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getTravelPackage().getId()));

        List<PackageRankingItem> unranked = byPackageId.values().stream()
                .map(pkgBookings -> {
                    TravelPackage pkg = pkgBookings.get(0).getTravelPackage();
                    long totalPassengers = pkgBookings.stream().mapToLong(Booking::getPassengerCount).sum();
                    BigDecimal totalAmount = sum(pkgBookings, Booking::getTotalAmount);
                    BigDecimal totalCollected = pkgBookings.stream()
                            .map(b -> paymentsByBookingId.get(b.getId()))
                            .filter(Objects::nonNull)
                            .map(Payment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new PackageRankingItem(
                            0, // el rank se asigna después de ordenar
                            pkg.getName(), pkg.getDestination(), pkgBookings.size(), totalPassengers,
                            totalAmount, totalCollected, pkg.getPrice());
                })
                .sorted(Comparator
                        .comparingLong(PackageRankingItem::getBookingCount).reversed()
                        .thenComparing(Comparator.comparingLong(PackageRankingItem::getTotalPassengers).reversed())
                        .thenComparing(Comparator.comparing(PackageRankingItem::getTotalAmount).reversed())
                        .thenComparing(PackageRankingItem::getPackageName))
                .toList();

        for (int i = 0; i < unranked.size(); i++) {
            unranked.get(i).setRank(i + 1);
        }
        return unranked;
    }

    /** Calcula los totales del ranking a partir de sus filas ya ordenadas. */
    private PackageRankingSummary buildRankingSummary(List<PackageRankingItem> ranking) {
        long totalBookings = ranking.stream().mapToLong(PackageRankingItem::getBookingCount).sum();
        long totalPassengers = ranking.stream().mapToLong(PackageRankingItem::getTotalPassengers).sum();
        BigDecimal totalAmount = sum(ranking, PackageRankingItem::getTotalAmount);
        return new PackageRankingSummary(ranking.size(), totalBookings, totalPassengers, totalAmount);
    }

    /** Trae los pagos de un conjunto de reservas en una sola consulta, indexados por bookingId. */
    private Map<Long, Payment> paymentsByBookingId(List<Booking> bookings) {
        List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();
        return paymentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(p -> p.getBooking().getId(), p -> p));
    }

    /** REGLA 1 y 2: ambas fechas son obligatorias, y el inicio no puede ser posterior al fin. */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("Start date and end date are both required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException("Start date must not be after end date");
        }
    }

    /** Suma un campo BigDecimal de una lista, tratando null como cero. */
    private <T> BigDecimal sum(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
        return items.stream()
                .map(extractor)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
