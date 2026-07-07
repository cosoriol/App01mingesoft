package com.travelagency.service;

import com.travelagency.dto.request.CreateBookingRequest;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.TravelPackageRepository;
import com.travelagency.repository.UserRepository;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * TESTS UNITARIOS DE BookingService (Épica 4)
 * =============================================
 * Cubre las 10 reglas de negocio del proceso de reserva.
 * Cada regla tiene al menos un test positivo (caso que debe
 * funcionar) y uno negativo (caso que debe fallar/rechazarse).
 *
 * Se mockean los repositorios (no se toca una base de datos real);
 * el objetivo es probar SOLO la lógica de BookingService.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TravelPackageRepository packageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    // ============================================================
    // HELPERS: construyen objetos de prueba con valores por defecto
    // ============================================================

    private User buildUser(Long id) {
        return User.builder()
                .id(id)
                .fullName("Cliente de Prueba")
                .email("cliente" + id + "@example.com")
                .role("CLIENT")
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
     * reserva "feliz": usuario y paquete existen, sin descuentos.
     */
    private void mockHappyPath(User user, TravelPackage travelPackage) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(packageRepository.findById(travelPackage.getId())).thenReturn(Optional.of(travelPackage));
        when(bookingRepository.countByUserIdAndStatus(anyLong(), any(BookingStatus.class))).thenReturn(0L);
        when(bookingRepository.countRecentBookingsByUser(anyLong(), any(LocalDateTime.class))).thenReturn(0L);
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
    void regla7_negativo_montoFinalNuncaEsNegativo_inclusoConTodosLosDescuentosAcumulados() {
        User user = buildUser(1L);
        TravelPackage travelPackage = buildPackage(1L, PackageStatus.AVAILABLE, 20, 0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(travelPackage));
        // Se activan los 3 descuentos a la vez:
        // grupo (>=4 pasajeros) 5% + cliente frecuente (>=3 confirmadas) 10%
        // + multi-paquete (>=1 reciente) 3% = 18% acumulado (bajo el tope de 20%)
        when(bookingRepository.countByUserIdAndStatus(anyLong(), any(BookingStatus.class))).thenReturn(5L);
        when(bookingRepository.countRecentBookingsByUser(anyLong(), any(LocalDateTime.class))).thenReturn(2L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.createBooking(1L, buildRequest(1L, 4));

        // El monto final jamás debe ser negativo, sin importar cuántos
        // descuentos se acumulen.
        assertTrue(result.getTotalAmount().compareTo(BigDecimal.ZERO) >= 0);
        // 18% acumulado sobre 400.00 = 72.00 de descuento -> total 328.00
        assertEquals(0, new BigDecimal("18.00").compareTo(result.getDiscountPercentage()));
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
}
