package com.travelagency.service;

import com.travelagency.dto.response.*;
import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import com.travelagency.entity.User;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * TESTS UNITARIOS DE ReportService (Épica 7: Generación de reportes)
 * ======================================================================
 * Cada reporte tiene al menos un test positivo y uno negativo/límite.
 * Se mockean BookingRepository y UserRepository (no se toca una base
 * de datos real). Todos los reportes ahora requieren un ADMIN (ver
 * AccessControlService) — corrección de un bug donde cualquiera podía
 * consultar ingresos y actividad del negocio sin autenticarse.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long ADMIN_ID = 1L;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        User admin = User.builder().id(ADMIN_ID).fullName("Admin").email("admin@example.com")
                .role("ADMIN").active(true).build();
        // lenient(): el test de control de acceso no usa este admin
        // (usa su propio cliente con id distinto), y eso no debería
        // marcarse como stub innecesario.
        lenient().when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        AccessControlService accessControlService = new AccessControlService(userRepository);
        reportService = new ReportService(bookingRepository, accessControlService);
    }

    private Booking buildBooking(BookingStatus status, BigDecimal baseAmount, BigDecimal totalAmount,
                                  BigDecimal discountAmount, String discountDetailsJson) {
        return Booking.builder()
                .status(status)
                .passengerCount(1)
                .baseAmount(baseAmount)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .discountDetails(discountDetailsJson)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // CONTROL DE ACCESO: SOLO ADMIN (aplica a los 4 reportes)
    // ============================================================

    @Test
    void controlDeAcceso_negativo_clienteNoPuedeVerNingunReporte() {
        User client = User.builder().id(2L).fullName("Cliente").email("c@example.com")
                .role("CLIENT").active(true).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class,
                () -> reportService.getSalesReport(2L, LocalDate.now().minusDays(1), LocalDate.now()));
        assertThrows(BusinessRuleException.class,
                () -> reportService.getMostBookedPackages(2L, 10));
        assertThrows(BusinessRuleException.class,
                () -> reportService.getBookingsSummary(2L));
        assertThrows(BusinessRuleException.class,
                () -> reportService.getDiscountEffectiveness(2L));
    }

    // ============================================================
    // REPORTE DE VENTAS POR PERÍODO
    // ============================================================

    @Test
    void ventas_positivo_soloContabilizaReservasConfirmed() {
        Booking confirmed1 = buildBooking(BookingStatus.CONFIRMED, new BigDecimal("100.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO, "[]");
        Booking confirmed2 = buildBooking(BookingStatus.CONFIRMED, new BigDecimal("200.00"),
                new BigDecimal("180.00"), new BigDecimal("20.00"), "[]");
        Booking pending = buildBooking(BookingStatus.PENDING, new BigDecimal("50.00"),
                new BigDecimal("50.00"), BigDecimal.ZERO, "[]");

        when(bookingRepository.findBookingsByDateRange(any(), any()))
                .thenReturn(List.of(confirmed1, confirmed2, pending));

        SalesReportResponse report = reportService.getSalesReport(
                ADMIN_ID, LocalDate.now().minusDays(30), LocalDate.now());

        assertEquals(2, report.getTotalConfirmedBookings()); // pending NO cuenta
        assertEquals(0, new BigDecimal("280.00").compareTo(report.getTotalRevenue()));
        assertEquals(0, new BigDecimal("20.00").compareTo(report.getTotalDiscountGiven()));
        assertEquals(0, new BigDecimal("140.00").compareTo(report.getAverageBookingValue()));
    }

    @Test
    void ventas_negativo_fechaInicioPosteriorAFin_lanzaBusinessRuleException() {
        assertThrows(BusinessRuleException.class,
                () -> reportService.getSalesReport(ADMIN_ID, LocalDate.now(), LocalDate.now().minusDays(1)));
    }

    @Test
    void ventas_negativo_sinReservasConfirmadas_devuelveCerosSinDividirPorCero() {
        when(bookingRepository.findBookingsByDateRange(any(), any())).thenReturn(List.of());

        SalesReportResponse report = reportService.getSalesReport(
                ADMIN_ID, LocalDate.now().minusDays(1), LocalDate.now());

        assertEquals(0, report.getTotalConfirmedBookings());
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getAverageBookingValue()));
    }

    // ============================================================
    // REPORTE DE PAQUETES MÁS RESERVADOS
    // ============================================================

    @Test
    void paquetesMasReservados_positivo_devuelveElRankingDelRepositorio() {
        List<PackagePopularityResponse> ranking = List.of(
                new PackagePopularityResponse(1L, "Machu Picchu", "Cusco, Perú", 5L, 12L));
        when(bookingRepository.findMostBookedPackages(any())).thenReturn(ranking);

        List<PackagePopularityResponse> result = reportService.getMostBookedPackages(ADMIN_ID, 10);

        assertEquals(1, result.size());
        assertEquals("Machu Picchu", result.get(0).getPackageName());
    }

    @Test
    void paquetesMasReservados_negativo_limiteCeroOMenor_lanzaBusinessRuleException() {
        assertThrows(BusinessRuleException.class, () -> reportService.getMostBookedPackages(ADMIN_ID, 0));
        assertThrows(BusinessRuleException.class, () -> reportService.getMostBookedPackages(ADMIN_ID, -5));
    }

    // ============================================================
    // RESUMEN DE RESERVAS POR ESTADO
    // ============================================================

    @Test
    void resumenPorEstado_positivo_sumaElTotalDeTodosLosEstados() {
        when(bookingRepository.countBookingsByStatus()).thenReturn(List.of(
                new BookingStatusCountResponse(BookingStatus.PENDING, 3L),
                new BookingStatusCountResponse(BookingStatus.CONFIRMED, 7L),
                new BookingStatusCountResponse(BookingStatus.CANCELLED, 2L)));

        BookingsSummaryResponse summary = reportService.getBookingsSummary(ADMIN_ID);

        assertEquals(12, summary.getTotalBookings());
        assertEquals(3, summary.getByStatus().size());
    }

    @Test
    void resumenPorEstado_negativo_sinReservas_devuelveTotalCero() {
        when(bookingRepository.countBookingsByStatus()).thenReturn(List.of());

        BookingsSummaryResponse summary = reportService.getBookingsSummary(ADMIN_ID);

        assertEquals(0, summary.getTotalBookings());
        assertTrue(summary.getByStatus().isEmpty());
    }

    // ============================================================
    // EFECTIVIDAD DE DESCUENTOS
    // ============================================================

    @Test
    void efectividadDescuentos_positivo_cuentaReservasConYSinDescuentoYAgrupaPorTipo() {
        String groupDiscountJson =
                "[{\"type\":\"GROUP\",\"description\":\"Descuento por grupo\",\"percentage\":5.0}]";
        Booking withDiscount1 = buildBooking(BookingStatus.CONFIRMED, new BigDecimal("400.00"),
                new BigDecimal("380.00"), new BigDecimal("20.00"), groupDiscountJson);
        Booking withDiscount2 = buildBooking(BookingStatus.PENDING, new BigDecimal("400.00"),
                new BigDecimal("380.00"), new BigDecimal("20.00"), groupDiscountJson);
        Booking withoutDiscount = buildBooking(BookingStatus.CONFIRMED, new BigDecimal("100.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO, "[]");

        when(bookingRepository.findByStatusNot(BookingStatus.CANCELLED))
                .thenReturn(List.of(withDiscount1, withDiscount2, withoutDiscount));

        DiscountEffectivenessResponse result = reportService.getDiscountEffectiveness(ADMIN_ID);

        assertEquals(3, result.getTotalBookingsAnalyzed());
        assertEquals(2, result.getBookingsWithDiscount());
        assertEquals(1, result.getBookingsWithoutDiscount());
        assertEquals(0, new BigDecimal("40.00").compareTo(result.getTotalDiscountGiven()));
        assertEquals(1, result.getByType().size());
        assertEquals(DiscountType.GROUP, result.getByType().get(0).getType());
        assertEquals(2, result.getByType().get(0).getTimesApplied());
        assertEquals(0, new BigDecimal("5.00").compareTo(result.getByType().get(0).getAveragePercentage()));
    }

    @Test
    void efectividadDescuentos_negativo_sinReservas_noRompeYDevuelveListaVacia() {
        when(bookingRepository.findByStatusNot(BookingStatus.CANCELLED)).thenReturn(List.of());

        DiscountEffectivenessResponse result = reportService.getDiscountEffectiveness(ADMIN_ID);

        assertEquals(0, result.getTotalBookingsAnalyzed());
        assertTrue(result.getByType().isEmpty());
    }
}
