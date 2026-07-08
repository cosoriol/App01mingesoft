package com.travelagency.service;

import com.travelagency.dto.response.*;
import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import com.travelagency.entity.PackageStatus;
import com.travelagency.entity.Payment;
import com.travelagency.entity.TravelPackage;
import com.travelagency.entity.User;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PaymentRepository;
import com.travelagency.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private PaymentRepository paymentRepository;

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
        reportService = new ReportService(bookingRepository, paymentRepository, accessControlService);

        // Por defecto, sin pagos ni reservas adicionales por fecha de
        // pago, salvo que un test específico lo sobrescriba. lenient()
        // porque no todos los tests llegan a consultar esto (ej. los
        // que fallan por validación de fechas antes de tocar los repos).
        lenient().when(paymentRepository.findByPaymentDateBetween(any(), any())).thenReturn(List.of());
        lenient().when(paymentRepository.findByBookingIdIn(anyList())).thenReturn(List.of());
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
    // HELPERS para el listado detallado de ventas y el ranking
    // ============================================================

    private User buildCustomer(Long id, String name, String email) {
        return User.builder().id(id).fullName(name).email(email).role("CLIENT").active(true).build();
    }

    private TravelPackage buildPackage(Long id, String name, String destination, BigDecimal price) {
        return TravelPackage.builder()
                .id(id).name(name).destination(destination).description("Descripción")
                .startDate(LocalDate.now().plusDays(10)).endDate(LocalDate.now().plusDays(15))
                .price(price).totalSlots(20).bookedSlots(0).status(PackageStatus.AVAILABLE)
                .build();
    }

    private Booking buildFullBooking(Long id, User user, TravelPackage pkg, int passengers,
                                      BookingStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {
        return Booking.builder()
                .id(id).user(user).travelPackage(pkg).passengerCount(passengers).status(status)
                .baseAmount(totalAmount).discountPercentage(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount).discountDetails("[]").createdAt(createdAt)
                .build();
    }

    private Payment buildPayment(Booking booking, BigDecimal amount) {
        return Payment.builder()
                .id(booking.getId()).booking(booking).amount(amount).paymentMethod("CREDIT_CARD")
                .cardLastFour("1234").paymentStatus("APPROVED").paymentDate(booking.getCreatedAt())
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

    // ============================================================
    // LISTADO DETALLADO DE VENTAS POR PERÍODO
    // ============================================================

    @Test
    void ventasDetallado_positivo_variasReservasEnElPeriodo() {
        User ana = buildCustomer(1L, "Ana Torres", "ana@example.com");
        User bruno = buildCustomer(2L, "Bruno Rojas", "bruno@example.com");
        TravelPackage machu = buildPackage(1L, "Machu Picchu", "Cusco, Perú", new BigDecimal("890.00"));
        TravelPackage cancun = buildPackage(2L, "Cancún", "Cancún, México", new BigDecimal("650.00"));

        Booking booking1 = buildFullBooking(1L, ana, machu, 2, BookingStatus.CONFIRMED,
                new BigDecimal("1780.00"), LocalDateTime.now().minusDays(2));
        Booking booking2 = buildFullBooking(2L, bruno, cancun, 1, BookingStatus.PENDING,
                new BigDecimal("650.00"), LocalDateTime.now().minusDays(1));

        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList()))
                .thenReturn(List.of(booking1, booking2));
        when(paymentRepository.findByBookingIdIn(anyList()))
                .thenReturn(List.of(buildPayment(booking1, new BigDecimal("1780.00"))));

        SalesListingResponse report = reportService.generateSalesReport(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now(), false);

        assertEquals(2, report.getSales().size());
        // Orden descendente por fecha de creación: booking2 (ayer) antes que booking1 (hace 2 días).
        assertEquals("Bruno Rojas", report.getSales().get(0).getCustomerName());
        assertEquals("Ana Torres", report.getSales().get(1).getCustomerName());
        assertEquals("Confirmada", report.getSales().get(1).getBookingStatus());
        assertEquals(0, new BigDecimal("1780.00").compareTo(report.getSales().get(1).getPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getSales().get(0).getPaidAmount())); // sin pago
    }

    @Test
    void ventasDetallado_negativo_periodoSinDatos_devuelveListaVaciaYTotalesEnCero() {
        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList()))
                .thenReturn(List.of());

        SalesListingResponse report = reportService.generateSalesReport(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now(), false);

        assertTrue(report.getSales().isEmpty());
        assertEquals(0, report.getSummary().getTotalBookings());
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getSummary().getTotalSalesAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(report.getSummary().getTotalCollectedAmount()));
        assertTrue(report.getSummary().getBookingsByStatus().isEmpty());
    }

    @Test
    void ventasDetallado_positivo_excluyeCanceladasPorDefecto() {
        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList()))
                .thenReturn(List.of());

        reportService.generateSalesReport(ADMIN_ID, LocalDate.now().minusDays(1), LocalDate.now(), false);

        ArgumentCaptor<List<BookingStatus>> excludedCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).findByCreatedAtBetweenAndStatusNotIn(any(), any(), excludedCaptor.capture());
        assertTrue(excludedCaptor.getValue().containsAll(List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED)));
        verify(bookingRepository, never()).findByCreatedAtBetween(any(), any());
    }

    @Test
    void ventasDetallado_positivo_incluyeCanceladasCuandoSePide() {
        User ana = buildCustomer(1L, "Ana Torres", "ana@example.com");
        TravelPackage machu = buildPackage(1L, "Machu Picchu", "Cusco, Perú", new BigDecimal("890.00"));
        Booking cancelled = buildFullBooking(1L, ana, machu, 2, BookingStatus.CANCELLED,
                new BigDecimal("1780.00"), LocalDateTime.now());

        when(bookingRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(cancelled));

        SalesListingResponse report = reportService.generateSalesReport(
                ADMIN_ID, LocalDate.now().minusDays(1), LocalDate.now(), true);

        assertEquals(1, report.getSales().size());
        assertEquals("Cancelada", report.getSales().get(0).getBookingStatus());
        verify(bookingRepository, never()).findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList());
    }

    @Test
    void ventasDetallado_positivo_incluyeReservaPorFechaDePagoAunqueCreacionQuedeFueraDelRango() {
        User ana = buildCustomer(1L, "Ana Torres", "ana@example.com");
        TravelPackage machu = buildPackage(1L, "Machu Picchu", "Cusco, Perú", new BigDecimal("890.00"));
        // Reserva creada MUCHO antes del período del reporte...
        Booking oldBooking = buildFullBooking(1L, ana, machu, 1, BookingStatus.CONFIRMED,
                new BigDecimal("890.00"), LocalDateTime.now().minusDays(60));
        // ...pero pagada DENTRO del período.
        Payment recentPayment = buildPayment(oldBooking, new BigDecimal("890.00"));

        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList()))
                .thenReturn(List.of()); // no calificaba por fecha de creación
        when(paymentRepository.findByPaymentDateBetween(any(), any())).thenReturn(List.of(recentPayment));
        when(paymentRepository.findByBookingIdIn(anyList())).thenReturn(List.of(recentPayment));

        SalesListingResponse report = reportService.generateSalesReport(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now(), false);

        assertEquals(1, report.getSales().size());
        assertEquals("Ana Torres", report.getSales().get(0).getCustomerName());
    }

    @Test
    void ventasDetallado_negativo_fechaInicioPosteriorAFin_lanzaBusinessRuleException() {
        assertThrows(BusinessRuleException.class,
                () -> reportService.generateSalesReport(
                        ADMIN_ID, LocalDate.now(), LocalDate.now().minusDays(1), false));
    }

    @Test
    void ventasDetallado_negativo_fechasNulas_lanzaBusinessRuleException() {
        assertThrows(BusinessRuleException.class,
                () -> reportService.generateSalesReport(ADMIN_ID, null, LocalDate.now(), false));
        assertThrows(BusinessRuleException.class,
                () -> reportService.generateSalesReport(ADMIN_ID, LocalDate.now(), null, false));
    }

    @Test
    void ventasDetallado_positivo_totalesDelResumenCalculadosCorrectamente() {
        User ana = buildCustomer(1L, "Ana Torres", "ana@example.com");
        User bruno = buildCustomer(2L, "Bruno Rojas", "bruno@example.com");
        TravelPackage machu = buildPackage(1L, "Machu Picchu", "Cusco, Perú", new BigDecimal("890.00"));

        Booking confirmed = buildFullBooking(1L, ana, machu, 2, BookingStatus.CONFIRMED,
                new BigDecimal("1780.00"), LocalDateTime.now().minusDays(1));
        Booking pending = buildFullBooking(2L, bruno, machu, 3, BookingStatus.PENDING,
                new BigDecimal("2670.00"), LocalDateTime.now());

        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList()))
                .thenReturn(List.of(confirmed, pending));
        when(paymentRepository.findByBookingIdIn(anyList()))
                .thenReturn(List.of(buildPayment(confirmed, new BigDecimal("1780.00"))));

        SalesListingResponse report = reportService.generateSalesReport(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now(), false);

        SalesReportSummary summary = report.getSummary();
        assertEquals(2, summary.getTotalBookings());
        assertEquals(5, summary.getTotalPassengers()); // 2 + 3
        assertEquals(0, new BigDecimal("4450.00").compareTo(summary.getTotalSalesAmount())); // 1780 + 2670
        assertEquals(0, new BigDecimal("1780.00").compareTo(summary.getTotalCollectedAmount())); // solo lo pagado
        assertEquals(1L, summary.getBookingsByStatus().get("Confirmada"));
        assertEquals(1L, summary.getBookingsByStatus().get("Pendiente de pago"));
    }

    // ============================================================
    // RANKING DE PAQUETES VENDIDOS POR PERÍODO
    // ============================================================

    @Test
    void ranking_positivo_variosPaquetesOrdenaCorrectamente() {
        User ana = buildCustomer(1L, "Ana", "ana@example.com");
        TravelPackage machu = buildPackage(1L, "Machu Picchu", "Cusco, Perú", new BigDecimal("890.00"));
        TravelPackage cancun = buildPackage(2L, "Cancún", "Cancún, México", new BigDecimal("650.00"));

        List<Booking> bookings = List.of(
                buildFullBooking(1L, ana, machu, 2, BookingStatus.CONFIRMED, new BigDecimal("1780.00"), LocalDateTime.now()),
                buildFullBooking(2L, ana, machu, 2, BookingStatus.CONFIRMED, new BigDecimal("1780.00"), LocalDateTime.now()),
                buildFullBooking(3L, ana, machu, 2, BookingStatus.CONFIRMED, new BigDecimal("1780.00"), LocalDateTime.now()),
                buildFullBooking(4L, ana, cancun, 1, BookingStatus.CONFIRMED, new BigDecimal("650.00"), LocalDateTime.now()));

        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList())).thenReturn(bookings);

        PackageRankingResponse response = reportService.generatePackageRanking(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals(2, response.getRanking().size());
        assertEquals(1, response.getRanking().get(0).getRank());
        assertEquals("Machu Picchu", response.getRanking().get(0).getPackageName());
        assertEquals(3, response.getRanking().get(0).getBookingCount());
        assertEquals(2, response.getRanking().get(1).getRank());
        assertEquals("Cancún", response.getRanking().get(1).getPackageName());
        assertEquals(2, response.getSummary().getTotalPackagesWithSales());
        assertEquals(4, response.getSummary().getTotalBookings());
    }

    @Test
    void ranking_positivo_empateEnReservas_desempataPorPasajeros() {
        User ana = buildCustomer(1L, "Ana", "ana@example.com");
        TravelPackage machu = buildPackage(1L, "Machu Picchu", "Cusco, Perú", new BigDecimal("890.00"));
        TravelPackage cancun = buildPackage(2L, "Cancún", "Cancún, México", new BigDecimal("650.00"));

        // Ambos paquetes con 2 reservas (empate), pero Machu Picchu suma más pasajeros.
        List<Booking> bookings = List.of(
                buildFullBooking(1L, ana, machu, 4, BookingStatus.CONFIRMED, new BigDecimal("3560.00"), LocalDateTime.now()),
                buildFullBooking(2L, ana, machu, 3, BookingStatus.CONFIRMED, new BigDecimal("2670.00"), LocalDateTime.now()),
                buildFullBooking(3L, ana, cancun, 1, BookingStatus.CONFIRMED, new BigDecimal("650.00"), LocalDateTime.now()),
                buildFullBooking(4L, ana, cancun, 2, BookingStatus.CONFIRMED, new BigDecimal("1300.00"), LocalDateTime.now()));

        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList())).thenReturn(bookings);

        PackageRankingResponse response = reportService.generatePackageRanking(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals("Machu Picchu", response.getRanking().get(0).getPackageName()); // 7 pasajeros vs 3
        assertEquals("Cancún", response.getRanking().get(1).getPackageName());
    }

    @Test
    void ranking_positivo_dobleEmpate_desempataPorMonto() {
        User ana = buildCustomer(1L, "Ana", "ana@example.com");
        TravelPackage machu = buildPackage(1L, "Machu Picchu", "Cusco, Perú", new BigDecimal("1000.00"));
        TravelPackage cancun = buildPackage(2L, "Cancún", "Cancún, México", new BigDecimal("500.00"));

        // Misma cantidad de reservas (1) y mismos pasajeros (2), pero Machu Picchu genera más dinero.
        List<Booking> bookings = List.of(
                buildFullBooking(1L, ana, machu, 2, BookingStatus.CONFIRMED, new BigDecimal("2000.00"), LocalDateTime.now()),
                buildFullBooking(2L, ana, cancun, 2, BookingStatus.CONFIRMED, new BigDecimal("1000.00"), LocalDateTime.now()));

        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList())).thenReturn(bookings);

        PackageRankingResponse response = reportService.generatePackageRanking(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals("Machu Picchu", response.getRanking().get(0).getPackageName());
        assertEquals("Cancún", response.getRanking().get(1).getPackageName());
    }

    @Test
    void ranking_positivo_tripleEmpate_desempataAlfabeticamente() {
        User ana = buildCustomer(1L, "Ana", "ana@example.com");
        TravelPackage zebra = buildPackage(1L, "Zebra Tour", "Africa", new BigDecimal("1000.00"));
        TravelPackage arco = buildPackage(2L, "Arco Iris Tour", "Perú", new BigDecimal("1000.00"));

        // Reservas idénticas (misma cantidad, mismos pasajeros, mismo monto) en ambos paquetes.
        List<Booking> bookings = List.of(
                buildFullBooking(1L, ana, zebra, 2, BookingStatus.CONFIRMED, new BigDecimal("2000.00"), LocalDateTime.now()),
                buildFullBooking(2L, ana, arco, 2, BookingStatus.CONFIRMED, new BigDecimal("2000.00"), LocalDateTime.now()));

        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList())).thenReturn(bookings);

        PackageRankingResponse response = reportService.generatePackageRanking(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals("Arco Iris Tour", response.getRanking().get(0).getPackageName()); // alfabético
        assertEquals("Zebra Tour", response.getRanking().get(1).getPackageName());
    }

    @Test
    void ranking_positivo_excluyeCanceladasYExpiradas() {
        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList()))
                .thenReturn(List.of());

        reportService.generatePackageRanking(ADMIN_ID, LocalDate.now().minusDays(1), LocalDate.now());

        ArgumentCaptor<List<BookingStatus>> excludedCaptor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository).findByCreatedAtBetweenAndStatusNotIn(any(), any(), excludedCaptor.capture());
        assertTrue(excludedCaptor.getValue().containsAll(List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED)));
    }

    @Test
    void ranking_negativo_periodoSinDatos_devuelveListaVacia() {
        when(bookingRepository.findByCreatedAtBetweenAndStatusNotIn(any(), any(), anyList()))
                .thenReturn(List.of());

        PackageRankingResponse response = reportService.generatePackageRanking(
                ADMIN_ID, LocalDate.now().minusDays(7), LocalDate.now());

        assertTrue(response.getRanking().isEmpty());
        assertEquals(0, response.getSummary().getTotalPackagesWithSales());
        assertEquals(0, response.getSummary().getTotalBookings());
    }

    @Test
    void ranking_negativo_fechaInicioPosteriorAFin_lanzaBusinessRuleException() {
        assertThrows(BusinessRuleException.class,
                () -> reportService.generatePackageRanking(
                        ADMIN_ID, LocalDate.now(), LocalDate.now().minusDays(1)));
    }

    @Test
    void ranking_negativo_fechasNulas_lanzaBusinessRuleException() {
        assertThrows(BusinessRuleException.class,
                () -> reportService.generatePackageRanking(ADMIN_ID, null, LocalDate.now()));
        assertThrows(BusinessRuleException.class,
                () -> reportService.generatePackageRanking(ADMIN_ID, LocalDate.now(), null));
    }
}
