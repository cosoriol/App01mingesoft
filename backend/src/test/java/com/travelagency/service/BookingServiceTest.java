package com.travelagency.service;

import com.travelagency.dto.request.CreateBookingRequest;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PaymentRepository;
import com.travelagency.repository.TravelPackageRepository;
import com.travelagency.repository.UserRepository;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * TESTS UNITARIOS DE BookingService (Épica 4)
 * =============================================
 * Cubre las 10 reglas de negocio del proceso de reserva.
 * Cada regla tiene al menos un test positivo (caso que debe
 * funcionar) y uno negativo (caso que debe fallar/rechazarse).
 *
 * Se mockean los repositorios y DiscountService (no se toca una base
 * de datos real); el objetivo es probar SOLO la lógica de
 * BookingService. La matemática real de los descuentos (montos,
 * porcentajes, tope, promociones) se prueba en DiscountServiceTest;
 * aquí solo se verifica que BookingService use correctamente lo que
 * DiscountService le devuelve.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TravelPackageRepository packageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DiscountService discountService;

    private BookingService bookingService;

    /**
     * BookingService ahora depende de AccessControlService (que a su
     * vez usa UserRepository). En vez de mockear AccessControlService,
     * se construye una instancia REAL apoyada en el mismo
     * userRepository ya mockeado — así los tests de owner/admin de
     * más abajo siguen funcionando exactamente igual que antes.
     */
    @BeforeEach
    void setUp() {
        AccessControlService accessControlService = new AccessControlService(userRepository);
        bookingService = new BookingService(
                bookingRepository, packageRepository, userRepository, paymentRepository,
                discountService, accessControlService);
    }

    // ============================================================
    // HELPERS: construyen objetos de prueba con valores por defecto
    // ============================================================

    private User buildUser(Long id) {
        return buildUser(id, "CLIENT");
    }

    private User buildUser(Long id, String role) {
        return User.builder()
                .id(id)
                .fullName("Usuario de Prueba " + id)
                .email("usuario" + id + "@example.com")
                .role(role)
                .active(true)
                .build();
    }

    private TravelPackage buildPackage(Long id, PackageStatus status, int totalSlots, int bookedSlots) {
        return TravelPackage.builder()
                .id(id)
                .name("Paquete de Prueba")
                .destination("Destino de Prueba")
                .description("Descripción de prueba")
                .startDate(java.time.LocalDate.now().plusDays(10))
                .endDate(java.time.LocalDate.now().plusDays(15))
                .price(new BigDecimal("100.00"))
                .totalSlots(totalSlots)
                .bookedSlots(bookedSlots)
                .status(status)
                .build();
    }

    private CreateBookingRequest buildRequest(Long packageId, Integer passengerCount) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setPackageId(packageId);
        request.setPassengerCount(passengerCount);
        return request;
    }

    /**
     * Configura los mocks compartidos para un flujo de creación de
     * reserva "feliz": usuario y paquete existen, sin descuentos
     * (DiscountService mockeado para devolver totalAmount = baseAmount).
     */
    private void mockHappyPath(User user, TravelPackage travelPackage) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(packageRepository.findById(travelPackage.getId())).thenReturn(Optional.of(travelPackage));
        when(discountService.calculateDiscounts(anyLong(), anyInt(), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    BigDecimal baseAmount = invocation.getArgument(2);
                    return new DiscountCalculationResult(
                            BigDecimal.ZERO, BigDecimal.ZERO, baseAmount, List.of(), "[]",
                            "No se aplicaron descuentos");
                });
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ============================================================
    // REGLA 1: USUARIO AUTENTICADO (debe existir en la BD)
    // ============================================================

    @Test
    void regla1_positivo_usuarioExistente_permiteReservar() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        mockHappyPath(user, travelPackage);

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 2));

        assertNotNull(result);
        verify(userRepository).findById(1L);
    }

    @Test
    void regla1_negativo_usuarioInexistente_lanzaResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(99L, buildRequest(1L, 2)));

        assertTrue(ex.getMessage().contains("User not found"));
        // No debe intentar buscar el paquete si el usuario no existe
        verify(packageRepository, never()).findById(any());
    }

    // ============================================================
    // REGLA 2: ASOCIACIÓN OBLIGATORIA (User y TravelPackage deben existir)
    // ============================================================

    @Test
    void regla2_positivo_reservaQuedaAsociadaAUsuarioYPaquete() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        mockHappyPath(user, travelPackage);

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 2));

        assertEquals(user, result.getUser());
        assertEquals(travelPackage, result.getTravelPackage());
    }

    @Test
    void regla2_negativo_paqueteInexistente_lanzaResourceNotFoundException() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(packageRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(1L, buildRequest(99L, 2)));

        assertTrue(ex.getMessage().contains("Package not found"));
    }

    // ============================================================
    // REGLA 3: PASAJEROS > 0
    // ============================================================

    @Test
    void regla3_positivo_pasajerosMayorACero_permiteReservar() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        mockHappyPath(user, travelPackage);

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 1));

        assertEquals(1, result.getPassengerCount());
    }

    @Test
    void regla3_negativo_pasajerosCero_lanzaBusinessRuleException() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(travelPackage));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> bookingService.createBooking(1L, buildRequest(1L, 0)));

        assertTrue(ex.getMessage().contains("greater than zero"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void regla3_negativo_pasajerosNegativo_lanzaBusinessRuleException() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(travelPackage));

        assertThrows(BusinessRuleException.class,
                () -> bookingService.createBooking(1L, buildRequest(1L, -3)));
    }

    // ============================================================
    // REGLA 4: VALIDACIÓN DE CUPOS DISPONIBLES
    // ============================================================

    @Test
    void regla4_positivo_pasajerosDentroDeCuposDisponibles_permiteReservar() {
        User user = buildUser(1L);
        // 10 cupos totales, 8 reservados -> quedan 2 disponibles
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 8);
        mockHappyPath(user, travelPackage);

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 2));

        assertNotNull(result);
    }

    @Test
    void regla4_negativo_pasajerosExcedenCuposDisponibles_lanzaBusinessRuleException() {
        User user = buildUser(1L);
        // 10 cupos totales, 9 reservados -> solo queda 1 disponible
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 9);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(travelPackage));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> bookingService.createBooking(1L, buildRequest(1L, 2)));

        assertTrue(ex.getMessage().contains("Not enough slots"));
    }

    // ============================================================
    // REGLA 5: DESCUENTO DE CUPOS AL RESERVAR (y transición a SOLD_OUT)
    // ============================================================

    @Test
    void regla5_positivo_reservaIncrementaBookedSlotsDelPaquete() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 2);
        mockHappyPath(user, travelPackage);

        bookingService.createBooking(1L, buildRequest(1L, 3));

        ArgumentCaptor<TravelPackage> captor = ArgumentCaptor.forClass(TravelPackage.class);
        verify(packageRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getBookedSlots()); // 2 + 3
    }

    @Test
    void regla5_negativo_alAgotarCuposElPaqueteCambiaAsoldOut() {
        User user = buildUser(1L);
        // Quedan exactamente 3 cupos disponibles
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 7);
        mockHappyPath(user, travelPackage);

        bookingService.createBooking(1L, buildRequest(1L, 3));

        ArgumentCaptor<TravelPackage> captor = ArgumentCaptor.forClass(TravelPackage.class);
        verify(packageRepository).save(captor.capture());
        assertEquals(PackageStatus.SOLD_OUT, captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getAvailableSlots());
    }

    // ============================================================
    // REGLA 6: ID ÚNICO AUTOGENERADO
    // ============================================================

    @Test
    void regla6_positivo_servicioNoAsignaIdManualmente_dejaQueJpaLoGenere() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        mockHappyPath(user, travelPackage);

        bookingService.createBooking(1L, buildRequest(1L, 2));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        // El id debe llegar en null al repositorio: es la BD (auto_increment)
        // quien lo asigna, nunca el servicio.
        assertNull(captor.getValue().getId());
    }

    @Test
    void regla6_negativo_entidadBookingDeclaraEstrategiaIdentityParaElId() throws NoSuchFieldException {
        // Verifica que la entidad esté configurada con @GeneratedValue(IDENTITY)
        // y no con una estrategia manual, garantizando unicidad automática.
        Field idField = Booking.class.getDeclaredField("id");
        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);

        assertNotNull(generatedValue, "El campo id debe tener @GeneratedValue");
        assertEquals(GenerationType.IDENTITY, generatedValue.strategy());
        assertNotEquals(GenerationType.TABLE, generatedValue.strategy());
    }

    // ============================================================
    // REGLA 7: CÁLCULO DEL MONTO (base, descuentos, total nunca negativo)
    // ============================================================

    @Test
    void regla7_positivo_sinDescuentos_totalAmountEsIgualABaseAmount() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        mockHappyPath(user, travelPackage); // 0 reservas previas -> sin descuentos

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 2));

        BigDecimal expectedBase = new BigDecimal("200.00"); // 100.00 x 2
        assertEquals(0, expectedBase.compareTo(result.getBaseAmount()));
        assertEquals(0, expectedBase.compareTo(result.getTotalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getDiscountAmount()));
    }

    @Test
    void regla7_negativo_bookingServiceUsaElResultadoDeDiscountServiceSinRecalcular() {
        // La matemática real de los descuentos (grupo + cliente frecuente +
        // multi-paquete + tope) se prueba en DiscountServiceTest. Aquí solo
        // verificamos que BookingService no recalcule nada por su cuenta:
        // debe usar EXACTAMENTE lo que DiscountService le entrega, incluso
        // cuando el monto final ya viene en 0 (el descuento "comió" todo).
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 20, 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(travelPackage));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiscountCalculationResult mockedResult = new DiscountCalculationResult(
                new BigDecimal("18.00"), new BigDecimal("72.00"), new BigDecimal("328.00"),
                List.of(), "[]", "Total: 18.00% (tope: 20.00%)");
        when(discountService.calculateDiscounts(eq(1L), eq(4), any(BigDecimal.class)))
                .thenReturn(mockedResult);

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 4));

        assertTrue(result.getTotalAmount().compareTo(BigDecimal.ZERO) >= 0);
        assertEquals(0, new BigDecimal("18.00").compareTo(result.getDiscountPercentage()));
        assertEquals(0, new BigDecimal("72.00").compareTo(result.getDiscountAmount()));
        assertEquals(0, new BigDecimal("328.00").compareTo(result.getTotalAmount()));
    }

    // ============================================================
    // REGLA 8: ESTADO INICIAL SIEMPRE PENDING
    // ============================================================

    @Test
    void regla8_positivo_reservaNuevaComienzaEnPending() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        mockHappyPath(user, travelPackage);

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 2));

        assertEquals(BookingStatus.PENDING, result.getStatus());
    }

    @Test
    void regla8_negativo_estadoInicialNoCambiaAunqueElPaqueteQuedeSoldOut() {
        User user = buildUser(1L);
        // Esta reserva agota los cupos restantes del paquete
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 5, 3);
        mockHappyPath(user, travelPackage);

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 2));

        // Aunque el PAQUETE pase a SOLD_OUT, la RESERVA sigue naciendo PENDING
        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertNotEquals(BookingStatus.CONFIRMED, result.getStatus());
    }

    // ============================================================
    // REGLA 9: SOLO SE PUEDE RESERVAR UN PAQUETE AVAILABLE
    // ============================================================

    @Test
    void regla9_positivo_paqueteAvailable_permiteReservar() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        mockHappyPath(user, travelPackage);

        assertDoesNotThrow(() -> bookingService.createBooking(1L, buildRequest(1L, 2)));
    }

    @ParameterizedTest(name = "regla9_negativo_paqueteEnEstado_{0}_rechazaReserva")
    @EnumSource(value = PackageStatus.class, names = {"CANCELLED", "EXPIRED", "SOLD_OUT"})
    void regla9_negativo_paqueteNoDisponible_lanzaBusinessRuleException(PackageStatus status) {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, status, 10, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(travelPackage));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> bookingService.createBooking(1L, buildRequest(1L, 2)));

        assertTrue(ex.getMessage().contains("not available"));
        verify(bookingRepository, never()).save(any());
    }

    // ============================================================
    // REGLA 10: EXPIRACIÓN AUTOMÁTICA DE RESERVAS PENDING VENCIDAS
    // ============================================================

    @Test
    void regla10_positivo_expiraReservaPendingVencidaYLiberaCupos() {
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.SOLD_OUT, 5, 5);
        Booking staleBooking = Booking.builder()
                .id(10L)
                .user(buildUser(1L))
                .travelPackage(travelPackage)
                .passengerCount(5)
                .baseAmount(new BigDecimal("500.00"))
                .totalAmount(new BigDecimal("500.00"))
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now().minusMinutes(45))
                .build();

        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(staleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int expiredCount = bookingService.expireStaleBookings();

        assertEquals(1, expiredCount);
        assertEquals(BookingStatus.EXPIRED, staleBooking.getStatus());
        assertEquals(0, travelPackage.getBookedSlots());
        assertEquals(PackageStatus.AVAILABLE, travelPackage.getStatus()); // vuelve a estar disponible
    }

    @Test
    void regla10_negativo_sinReservasVencidas_noExpiraNadaNiTocaElPaquete() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int expiredCount = bookingService.expireStaleBookings();

        assertEquals(0, expiredCount);
        verify(bookingRepository, never()).save(any());
        verify(packageRepository, never()).save(any());
    }

    // ============================================================
    // CANCELACIÓN DE RESERVAS (Épica 6: seguimiento de reservas)
    // ============================================================

    private Booking buildBooking(Long id, User owner, TravelPackage travelPackage,
                                  int passengerCount, BookingStatus status) {
        return Booking.builder()
                .id(id)
                .user(owner)
                .travelPackage(travelPackage)
                .passengerCount(passengerCount)
                .baseAmount(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("100.00"))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void cancelacion_positivo_dueñoCancelaSuPropiaReservaPending_liberaCupos() {
        User owner = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 3);
        Booking booking = buildBooking(5L, owner, travelPackage, 3, BookingStatus.PENDING);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(5L, 1L);

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        assertEquals(0, travelPackage.getBookedSlots());
        verify(paymentRepository, never()).findByBookingId(any()); // no estaba pagada, no hay nada que reembolsar
    }

    @Test
    void cancelacion_positivo_adminPuedeCancelarReservaDeOtroUsuario() {
        User owner = buildUser(1L);
        User admin = buildUser(2L, "ADMIN");
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 2);
        Booking booking = buildBooking(5L, owner, travelPackage, 2, BookingStatus.PENDING);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> bookingService.cancelBooking(5L, 2L));
    }

    @Test
    void cancelacion_negativo_usuarioNoDueñoNiAdmin_lanzaBusinessRuleException() {
        User owner = buildUser(1L);
        User intruso = buildUser(2L, "CLIENT");
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 2);
        Booking booking = buildBooking(5L, owner, travelPackage, 2, BookingStatus.PENDING);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> bookingService.cancelBooking(5L, 2L));

        assertTrue(ex.getMessage().contains("not allowed"));
        verify(packageRepository, never()).save(any());
    }

    @Test
    void cancelacion_negativo_reservaYaCancelada_lanzaBusinessRuleException() {
        User owner = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        Booking booking = buildBooking(5L, owner, travelPackage, 2, BookingStatus.CANCELLED);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThrows(BusinessRuleException.class, () -> bookingService.cancelBooking(5L, 1L));
    }

    @Test
    void cancelacion_negativo_bugFix_reservaExpirada_noPermiteCancelarNiDescontarCuposDeNuevo() {
        // Esta reserva ya fue expirada por el scheduler (Regla 10 de la Épica 4),
        // que ya liberó sus cupos. Antes del fix, cancelarla volvía a restar
        // los cupos, dejando bookedSlots en negativo.
        User owner = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        Booking booking = buildBooking(5L, owner, travelPackage, 2, BookingStatus.EXPIRED);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> bookingService.cancelBooking(5L, 1L));

        assertTrue(ex.getMessage().contains("already expired"));
        verify(packageRepository, never()).save(any());
        assertEquals(0, travelPackage.getBookedSlots()); // los cupos NO se tocan de nuevo
    }

    @Test
    void cancelacion_positivo_reservaConfirmadaYaPagada_marcaElPagoComoReembolsado() {
        User owner = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 2);
        Booking booking = buildBooking(5L, owner, travelPackage, 2, BookingStatus.CONFIRMED);
        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .paymentStatus("APPROVED")
                .build();

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBookingId(5L)).thenReturn(Optional.of(payment));

        bookingService.cancelBooking(5L, 1L);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertEquals("REFUNDED", captor.getValue().getPaymentStatus());
    }

    // ============================================================
    // FILTRO DE RESERVAS POR ESTADO (Épica 6: seguimiento de reservas)
    // ============================================================

    @Test
    void filtroEstado_positivo_devuelveSoloReservasDelEstadoSolicitado() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        Booking confirmedBooking = buildBooking(1L, user, travelPackage, 2, BookingStatus.CONFIRMED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findByUserIdAndStatus(1L, BookingStatus.CONFIRMED))
                .thenReturn(List.of(confirmedBooking));

        List<Booking> result = bookingService.getBookingsByUserAndStatus(1L, BookingStatus.CONFIRMED, 1L);

        assertEquals(1, result.size());
        assertEquals(BookingStatus.CONFIRMED, result.get(0).getStatus());
    }

    @Test
    void filtroEstado_negativo_sinReservasEnEseEstado_devuelveListaVacia() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findByUserIdAndStatus(1L, BookingStatus.CANCELLED))
                .thenReturn(List.of());

        List<Booking> result = bookingService.getBookingsByUserAndStatus(1L, BookingStatus.CANCELLED, 1L);

        assertTrue(result.isEmpty());
    }

    // ============================================================
    // CONTROL DE ACCESO EN LECTURA (Épica 6: corrección de bug de
    // privacidad — antes cualquiera veía las reservas de cualquiera)
    // ============================================================

    @Test
    void getBookingsByUser_positivo_elPropioUsuarioVeSuHistorial() {
        User user = buildUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> bookingService.getBookingsByUser(1L, 1L));
    }

    @Test
    void getBookingsByUser_positivo_adminVeElHistorialDeOtroUsuario() {
        User admin = buildUser(2L, "ADMIN");
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> bookingService.getBookingsByUser(1L, 2L));
    }

    @Test
    void getBookingsByUser_negativo_otroClienteNoPuedeVerHistorialAjeno() {
        User intruso = buildUser(2L, "CLIENT");
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        assertThrows(BusinessRuleException.class, () -> bookingService.getBookingsByUser(1L, 2L));
        verify(bookingRepository, never()).findByUserId(any());
    }

    @Test
    void getAllBookings_positivo_adminPuedeVerTodas() {
        User admin = buildUser(1L, "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(bookingRepository.findAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> bookingService.getAllBookings(1L));
    }

    @Test
    void getAllBookings_negativo_clienteNoPuedeVerTodas() {
        User client = buildUser(1L, "CLIENT");
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class, () -> bookingService.getAllBookings(1L));
        verify(bookingRepository, never()).findAll();
    }

    @Test
    void getBookingByIdForRequester_positivo_elDueñoVeSuPropiaReserva() {
        User owner = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 2);
        Booking booking = buildBooking(5L, owner, travelPackage, 2, BookingStatus.PENDING);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertDoesNotThrow(() -> bookingService.getBookingByIdForRequester(5L, 1L));
    }

    @Test
    void getBookingByIdForRequester_negativo_otroClienteNoPuedeVerReservaAjena() {
        User owner = buildUser(1L);
        User intruso = buildUser(2L, "CLIENT");
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 10, 2);
        Booking booking = buildBooking(5L, owner, travelPackage, 2, BookingStatus.PENDING);
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        assertThrows(BusinessRuleException.class,
                () -> bookingService.getBookingByIdForRequester(5L, 2L));
    }
}
