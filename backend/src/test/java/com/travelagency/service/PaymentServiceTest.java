package com.travelagency.service;

import com.travelagency.dto.request.PaymentRequest;
import com.travelagency.dto.response.PaymentSummaryResponse;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TESTS UNITARIOS DE PaymentService (Épica 5: Gestión de pagos en línea)
 * ==========================================================================
 * Cubre las 12 reglas de negocio del pago simulado. Cada regla tiene
 * al menos un test positivo y uno negativo. Se mockean los
 * repositorios (no se toca una base de datos real).
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    /** Dueño por defecto de las reservas de prueba (ver buildBooking). */
    private static final Long OWNER_ID = 1L;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    private PaymentService paymentService;

    /**
     * PaymentService depende de AccessControlService (que usa
     * UserRepository) para processPayment y las dos consultas de
     * solo lectura. Se construye una instancia REAL apoyada en el
     * mismo userRepository ya mockeado, en vez de mockear
     * AccessControlService.
     */
    @BeforeEach
    void setUp() {
        AccessControlService accessControlService = new AccessControlService(userRepository);
        paymentService = new PaymentService(paymentRepository, bookingRepository, accessControlService);

        // Dueño por defecto: la mayoría de los tests pagan/consultan
        // como el propio dueño de la reserva (OWNER_ID). lenient()
        // porque algunos tests (ej. reserva inexistente) nunca llegan
        // a necesitar este usuario.
        User owner = User.builder().id(OWNER_ID).fullName("Ana Torres").email("ana@example.com")
                .role("CLIENT").active(true).build();
        lenient().when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /** Fecha de expiración siempre válida (2 años en el futuro), en formato MM/YY. */
    private static String futureExpiration() {
        return YearMonth.now().plusYears(2).format(DateTimeFormatter.ofPattern("MM/yy"));
    }

    /** Reserva de prueba, siempre perteneciente a OWNER_ID. */
    private Booking buildBooking(Long id, BookingStatus status, BigDecimal totalAmount) {
        return Booking.builder()
                .id(id)
                .user(User.builder().id(OWNER_ID).fullName("Ana Torres").email("ana@example.com")
                        .role("CLIENT").active(true).build())
                .passengerCount(2)
                .baseAmount(totalAmount)
                .totalAmount(totalAmount)
                .status(status)
                .build();
    }

    private PaymentRequest buildValidRequest(Long bookingId, BigDecimal amount) {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(bookingId);
        request.setAmount(amount);
        request.setPaymentMethod("CREDIT_CARD");
        request.setCardNumber("4111111111111111");
        request.setExpirationDate(futureExpiration());
        request.setCvv("123");
        request.setCardHolderName("Ana Torres");
        return request;
    }

    private void mockHappyPath(Booking booking) {
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ============================================================
    // REGLA 1: PAGO ASOCIADO A RESERVA EXISTENTE
    // ============================================================

    @Test
    void regla1_positivo_reservaExistente_permiteProcesarElPago() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        assertDoesNotThrow(() -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));
    }

    @Test
    void regla1_negativo_reservaInexistente_lanzaResourceNotFoundException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.processPayment(OWNER_ID, buildValidRequest(99L, new BigDecimal("100.00"))));

        assertTrue(ex.getMessage().contains("Booking not found"));
        verify(paymentRepository, never()).save(any());
    }

    // ============================================================
    // CONTROL DE ACCESO: SOLO EL DUEÑO DE LA RESERVA (O UN ADMIN)
    // PUEDE PAGARLA — corrección de bug: antes cualquiera con datos
    // de tarjeta simulados podía pagar (y confirmar) la reserva ajena.
    // ============================================================

    @Test
    void controlDeAcceso_positivo_elDueñoPuedePagarSuPropiaReserva() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        assertDoesNotThrow(() ->
                paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));
    }

    @Test
    void controlDeAcceso_positivo_unAdminPuedePagarLaReservaDeOtroUsuario() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);
        User admin = User.builder().id(9L).fullName("Admin").email("admin@example.com")
                .role("ADMIN").active(true).build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() ->
                paymentService.processPayment(9L, buildValidRequest(1L, new BigDecimal("100.00"))));
    }

    @Test
    void controlDeAcceso_negativo_otroClienteNoPuedePagarUnaReservaAjena() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        User intruso = User.builder().id(2L).fullName("Intruso").email("i@example.com")
                .role("CLIENT").active(true).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(2L, buildValidRequest(1L, new BigDecimal("100.00"))));

        assertTrue(ex.getMessage().contains("not allowed"));
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    // ============================================================
    // REGLA 2: NO PAGAR RESERVA CANCELLED O EXPIRED
    // ============================================================

    @Test
    void regla2_negativo_reservaCancelada_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.CANCELLED, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));

        assertTrue(ex.getMessage().contains("cancelled"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void regla2_negativo_reservaExpirada_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.EXPIRED, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));

        assertTrue(ex.getMessage().contains("expired"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void regla2_positivo_reservaPending_permiteElPago() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        assertDoesNotThrow(() -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));
    }

    // ============================================================
    // REGLA 3: MONTO MAYOR A CERO
    // ============================================================

    @Test
    void regla3_negativo_montoCero_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, BigDecimal.ZERO);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, BigDecimal.ZERO)));
    }

    @Test
    void regla3_positivo_montoMayorACero_permiteElPago() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("50.00"));
        mockHappyPath(booking);

        assertDoesNotThrow(() -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("50.00"))));
    }

    // ============================================================
    // REGLA 4: EL MONTO DEBE COINCIDIR EXACTO CON EL TOTAL (sin cuotas)
    // ============================================================

    @Test
    void regla4_negativo_montoNoCoincideConElTotal_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("50.00"))));

        assertTrue(ex.getMessage().contains("does not match"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void regla4_positivo_montoCoincideExactoConElTotal_permiteElPago() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        assertDoesNotThrow(() -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));
    }

    // ============================================================
    // REGLA 5: UN SOLO PAGO POR RESERVA
    // ============================================================

    @Test
    void regla5_negativo_pagoDuplicado_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingId(1L)).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void regla5_positivo_sinPagoPrevio_permiteElPago() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingId(1L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));
    }

    // ============================================================
    // REGLA 6: CAMBIO AUTOMÁTICO DE ESTADO A CONFIRMED
    // ============================================================

    @Test
    void regla6_positivo_reservaCambiaAConfirmedTrasElPago() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00")));

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertEquals(BookingStatus.CONFIRMED, captor.getValue().getStatus());
    }

    @Test
    void regla6_negativo_siElPagoFallaLaReservaNoCambiaDeEstado() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        PaymentRequest invalidRequest = buildValidRequest(1L, new BigDecimal("999.00")); // no coincide (Regla 4)
        assertThrows(BusinessRuleException.class, () -> paymentService.processPayment(OWNER_ID, invalidRequest));

        assertEquals(BookingStatus.PENDING, booking.getStatus()); // sigue igual, nunca se confirmó
        verify(bookingRepository, never()).save(any());
    }

    // ============================================================
    // REGLA 7: DATOS DE LA TRANSACCIÓN (happy path completo)
    // ============================================================

    @Test
    void regla7_positivo_pagoExitoso_guardaLosDatosDeLaTransaccion() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("890.00"));
        mockHappyPath(booking);

        PaymentRequest request = buildValidRequest(1L, new BigDecimal("890.00"));
        request.setCardNumber("4111111111111234");

        Payment result = paymentService.processPayment(OWNER_ID, request);

        assertEquals(0, new BigDecimal("890.00").compareTo(result.getAmount()));
        assertEquals("CREDIT_CARD", result.getPaymentMethod());
        assertEquals("1234", result.getCardLastFour()); // solo los últimos 4 dígitos
        assertEquals("Ana Torres", result.getCardHolderName());
        // paymentDate lo asigna @PrePersist (JPA) al guardar en la BD real;
        // en este test unitario (repositorio mockeado) no se dispara, por
        // eso no se afirma nada sobre su valor aquí.
        assertNull(result.getId()); // el id lo genera la BD (auto_increment), no el servicio
    }

    @Test
    void regla7_negativo_nuncaSeGuardaElNumeroCompletoDeLaTarjeta() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        PaymentRequest request = buildValidRequest(1L, new BigDecimal("100.00"));
        request.setCardNumber("4111111111111111");

        Payment result = paymentService.processPayment(OWNER_ID, request);

        assertNotEquals(request.getCardNumber(), result.getCardLastFour());
        assertEquals(4, result.getCardLastFour().length());
    }

    // ============================================================
    // REGLA 8: MEDIO DE PAGO VÁLIDO (solo CREDIT_CARD)
    // ============================================================

    @Test
    void regla8_negativo_medioDePagoNoSoportado_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        PaymentRequest request = buildValidRequest(1L, new BigDecimal("100.00"));
        request.setPaymentMethod("DEBIT_CARD");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, request));

        assertTrue(ex.getMessage().contains("Unsupported payment method"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void regla8_positivo_creditCard_esAceptado() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        Payment result = paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00")));

        assertEquals("CREDIT_CARD", result.getPaymentMethod());
    }

    // ============================================================
    // REGLA 9: VALIDACIONES DE FORMATO DE TARJETA
    // ============================================================

    @Test
    void regla9_negativo_numeroDeTarjetaInvalido_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        PaymentRequest request = buildValidRequest(1L, new BigDecimal("100.00"));
        request.setCardNumber("12345"); // no son 16 dígitos

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, request));

        assertTrue(ex.getMessage().contains("16 digits"));
    }

    @Test
    void regla9_negativo_cvvInvalido_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        PaymentRequest request = buildValidRequest(1L, new BigDecimal("100.00"));
        request.setCvv("12"); // no son 3 dígitos

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, request));

        assertTrue(ex.getMessage().contains("3 digits"));
    }

    @Test
    void regla9_negativo_fechaExpiracionVencida_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        PaymentRequest request = buildValidRequest(1L, new BigDecimal("100.00"));
        request.setExpirationDate(YearMonth.now().minusYears(1).format(DateTimeFormatter.ofPattern("MM/yy")));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> paymentService.processPayment(OWNER_ID, request));

        assertTrue(ex.getMessage().contains("already passed"));
    }

    @Test
    void regla9_negativo_formatoDeFechaInvalido_lanzaBusinessRuleException() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        PaymentRequest request = buildValidRequest(1L, new BigDecimal("100.00"));
        request.setExpirationDate("2028-01"); // formato incorrecto, no MM/YY

        assertThrows(BusinessRuleException.class, () -> paymentService.processPayment(OWNER_ID, request));
    }

    @Test
    void regla9_positivo_datosDeTarjetaValidos_permiteElPago() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        assertDoesNotThrow(() -> paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00"))));
    }

    // ============================================================
    // REGLA 10: PAGO SIEMPRE EXITOSO (APPROVED, sin simular rechazos)
    // ============================================================

    @Test
    void regla10_positivo_pagoQuePasaValidaciones_quedaSiempreApproved() {
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        mockHappyPath(booking);

        Payment result = paymentService.processPayment(OWNER_ID, buildValidRequest(1L, new BigDecimal("100.00")));

        assertEquals("APPROVED", result.getPaymentStatus());
    }

    // ============================================================
    // REGLA 11: RESUMEN DE LA RESERVA ANTES DE PAGAR
    // ============================================================

    @Test
    void regla11_positivo_dueñoDeLaReservaVeElResumenAntesDePagar() {
        User owner = User.builder().id(1L).fullName("Ana Torres").email("ana@example.com")
                .role("CLIENT").active(true).build();
        TravelPackage travelPackage = TravelPackage.builder()
                .id(1L).name("Aventura en Machu Picchu").destination("Cusco, Perú")
                .price(new BigDecimal("890.00")).totalSlots(10).bookedSlots(0)
                .status(PackageStatus.AVAILABLE)
                .build();
        Booking booking = Booking.builder()
                .id(1L).user(owner).travelPackage(travelPackage).passengerCount(2)
                .baseAmount(new BigDecimal("1780.00"))
                .discountPercentage(new BigDecimal("5.0"))
                .discountAmount(new BigDecimal("89.00"))
                .totalAmount(new BigDecimal("1691.00"))
                .discountDetails("[]")
                .discountSummary("Descuento por grupo (4+ pasajeros): 5.0% | Total: 5.0% (tope: 20.0%)")
                .status(BookingStatus.PENDING)
                .build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        PaymentSummaryResponse summary = paymentService.getPaymentSummary(1L, 1L);

        assertEquals("Aventura en Machu Picchu", summary.getPackageName());
        assertEquals("Cusco, Perú", summary.getDestination());
        assertEquals(2, summary.getPassengerCount());
        assertEquals(0, new BigDecimal("1780.00").compareTo(summary.getBaseAmount()));
        assertEquals(0, new BigDecimal("1691.00").compareTo(summary.getTotalAmount()));
        assertEquals(BookingStatus.PENDING, summary.getBookingStatus());
    }

    @Test
    void regla11_negativo_reservaInexistente_lanzaResourceNotFoundException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentSummary(99L, 1L));
    }

    @Test
    void regla11_negativo_otroClienteNoPuedeVerElResumenDeUnaReservaAjena() {
        // Corrección de bug: antes cualquiera veía el monto a pagar de
        // cualquier reserva con solo saber el ID.
        User owner = User.builder().id(1L).fullName("Ana Torres").email("ana@example.com")
                .role("CLIENT").active(true).build();
        User intruso = User.builder().id(2L).fullName("Intruso").email("i@example.com")
                .role("CLIENT").active(true).build();
        Booking booking = buildBooking(1L, BookingStatus.PENDING, new BigDecimal("100.00"));
        booking.setUser(owner);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        assertThrows(BusinessRuleException.class, () -> paymentService.getPaymentSummary(1L, 2L));
    }

    // ============================================================
    // REGLA 12 (soporte): OBTENER PAGO DE UNA RESERVA
    // ============================================================

    @Test
    void obtenerPago_positivo_elDueñoVeSuPropioPago() {
        User owner = User.builder().id(1L).fullName("Ana Torres").email("ana@example.com")
                .role("CLIENT").active(true).build();
        Booking booking = buildBooking(1L, BookingStatus.CONFIRMED, new BigDecimal("100.00"));
        booking.setUser(owner);
        Payment payment = Payment.builder()
                .id(5L).booking(booking).amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD").cardLastFour("1111")
                .cardHolderName("Ana Torres").paymentStatus("APPROVED")
                .build();
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        Payment result = paymentService.getPaymentByBookingId(1L, 1L);

        assertEquals(5L, result.getId());
    }

    @Test
    void obtenerPago_negativo_sinPagoRegistrado_lanzaResourceNotFoundException() {
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentByBookingId(1L, 1L));
    }

    @Test
    void obtenerPago_negativo_otroClienteNoPuedeVerElPagoDeUnaReservaAjena() {
        // Corrección de bug: antes cualquiera veía el monto pagado y
        // los últimos 4 dígitos de la tarjeta de cualquier reserva.
        User owner = User.builder().id(1L).fullName("Ana Torres").email("ana@example.com")
                .role("CLIENT").active(true).build();
        User intruso = User.builder().id(2L).fullName("Intruso").email("i@example.com")
                .role("CLIENT").active(true).build();
        Booking booking = buildBooking(1L, BookingStatus.CONFIRMED, new BigDecimal("100.00"));
        booking.setUser(owner);
        Payment payment = Payment.builder()
                .id(5L).booking(booking).amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD").cardLastFour("1111")
                .paymentStatus("APPROVED")
                .build();
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        assertThrows(BusinessRuleException.class, () -> paymentService.getPaymentByBookingId(1L, 2L));
    }
}
