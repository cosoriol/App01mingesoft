package com.travelagency.service;

import com.travelagency.dto.request.CreateBookingRequest;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PaymentRepository;
import com.travelagency.repository.TravelPackageRepository;
import com.travelagency.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SERVICIO DE RESERVAS (Capa Service)
 * =====================================
 * Contiene la lógica de negocio más compleja del sistema:
 * crear reservas, validar cupos, coordinar el cálculo de descuentos.
 *
 * Épica 4: Proceso de reserva en línea
 *
 * El CÁLCULO de descuentos ya no vive aquí: se delega por completo a
 * DiscountService (responsabilidad única). BookingService solo arma
 * la reserva con el resultado que DiscountService le entrega.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TravelPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final DiscountService discountService;
    private final AccessControlService accessControlService;

    /** Minutos tras los cuales una reserva PENDING sin pagar expira automáticamente */
    private static final long PENDING_EXPIRATION_MINUTES = 30;

    /**
     * CREAR UNA RESERVA
     * ===================
     * Este es el método principal. Implementa las 10 reglas de la
     * Épica 4 (cada una marcada explícitamente con un comentario
     * "REGLA N" para que sea fácil ubicarlas):
     *
     * REGLA 1 y 2: el usuario y el paquete deben existir
     * REGLA 9:  el paquete debe estar AVAILABLE
     * REGLA 3:  los pasajeros deben ser mayor que cero
     * REGLA 4:  debe haber cupos suficientes
     * REGLA 7:  cálculo de montos y descuentos
     * REGLA 6 y 8: id autogenerado y estado inicial PENDING
     * REGLA 5:  descuento de cupos del paquete
     *
     * @param userId  ID del usuario que hace la reserva
     * @param request datos de la reserva (packageId + passengerCount)
     * @return la reserva creada
     */
    public Booking createBooking(Long userId, CreateBookingRequest request) {

        // REGLA 1: solo un usuario existente (autenticado) puede reservar.
        // REGLA 2: la reserva debe asociarse a un User real.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        // REGLA 2: la reserva debe asociarse a un TravelPackage real.
        TravelPackage travelPackage = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Package not found with id: " + request.getPackageId()));

        // REGLA 9: solo se puede reservar un paquete en estado AVAILABLE.
        // Esto bloquea automáticamente CANCELLED, EXPIRED y SOLD_OUT.
        if (travelPackage.getStatus() != PackageStatus.AVAILABLE) {
            throw new BusinessRuleException(
                    "Package is not available for booking. Current status: " + travelPackage.getStatus());
        }

        // REGLA 3: la cantidad de pasajeros debe ser mayor que cero.
        if (request.getPassengerCount() == null || request.getPassengerCount() <= 0) {
            throw new BusinessRuleException("Passenger count must be greater than zero");
        }

        // REGLA 4: no se puede reservar más cupos de los disponibles.
        int availableSlots = travelPackage.getAvailableSlots();
        if (request.getPassengerCount() > availableSlots) {
            throw new BusinessRuleException(
                    "Not enough slots. Requested: " + request.getPassengerCount()
                    + ", Available: " + availableSlots);
        }

        // REGLA 6 (paso 1): monto base = precio por persona × cantidad de pasajeros.
        BigDecimal pricePerPerson = travelPackage.getPrice();
        BigDecimal baseAmount = pricePerPerson.multiply(
                BigDecimal.valueOf(request.getPassengerCount()));

        // REGLA 6 (pasos 2-9): todo el cálculo de descuentos (grupo, cliente
        // frecuente, multi-paquete, promociones, tope máximo, monto final)
        // se delega a DiscountService.
        DiscountCalculationResult discountResult = discountService.calculateDiscounts(
                userId, request.getPassengerCount(), baseAmount);

        // REGLA 6: el id se genera automáticamente al guardar (@GeneratedValue en Booking).
        // REGLA 8: toda reserva nueva comienza en estado PENDING.
        Booking booking = Booking.builder()
                .user(user)
                .travelPackage(travelPackage)
                .passengerCount(request.getPassengerCount())
                .baseAmount(baseAmount)
                .discountPercentage(discountResult.totalPercentage())
                .discountAmount(discountResult.discountAmount())
                .totalAmount(discountResult.totalAmount())
                .discountDetails(discountResult.discountDetailsJson())
                .discountSummary(discountResult.discountSummary())
                .status(BookingStatus.PENDING)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // REGLA 5: descontar los cupos utilizados del paquete.
        travelPackage.setBookedSlots(
                travelPackage.getBookedSlots() + request.getPassengerCount());

        // REGLA 5: si se agotaron los cupos, el paquete pasa a SOLD_OUT.
        if (travelPackage.getAvailableSlots() <= 0) {
            travelPackage.setStatus(PackageStatus.SOLD_OUT);
        }

        packageRepository.save(travelPackage);

        return savedBooking;
    }

    /**
     * OBTENER RESERVAS DE UN USUARIO (Épica 6)
     *
     * Corrección de bug: antes cualquiera podía ver el historial de
     * reservas de CUALQUIER usuario con solo cambiar el id en la URL.
     * Ahora se exige que quien pregunta sea ese mismo usuario o un ADMIN.
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(Long userId, Long requestingUserId) {
        accessControlService.requireOwnerOrAdmin(requestingUserId, userId);
        return bookingRepository.findByUserId(userId);
    }

    /**
     * OBTENER RESERVAS DE UN USUARIO FILTRADAS POR ESTADO (Épica 6)
     * Permite al cliente hacer seguimiento de un grupo específico de
     * reservas, por ejemplo "solo mis reservas confirmadas".
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUserAndStatus(Long userId, BookingStatus status, Long requestingUserId) {
        accessControlService.requireOwnerOrAdmin(requestingUserId, userId);
        return bookingRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * OBTENER TODAS LAS RESERVAS (para administradores, Épica 6)
     *
     * Corrección de bug: antes no verificaba nada — cualquiera podía
     * volcar las reservas de TODOS los clientes.
     */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings(Long adminUserId) {
        accessControlService.requireAdmin(adminUserId);
        return bookingRepository.findAll();
    }

    /**
     * OBTENER UNA RESERVA POR ID (uso interno, sin control de acceso).
     * La usan cancelBooking/expireStaleBookings, que hacen su propia
     * verificación (o no la necesitan, por ser tareas del sistema).
     */
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + id));
    }

    /**
     * OBTENER UNA RESERVA POR ID, para un endpoint público.
     *
     * Corrección de bug: antes cualquiera podía ver el detalle
     * financiero de cualquier reserva adivinando el ID.
     */
    @Transactional(readOnly = true)
    public Booking getBookingByIdForRequester(Long id, Long requestingUserId) {
        Booking booking = getBookingById(id);
        accessControlService.requireOwnerOrAdmin(requestingUserId, booking.getUser().getId());
        return booking;
    }

    /**
     * CANCELAR UNA RESERVA (Épica 6)
     * ================================
     * Al cancelar, se liberan los cupos del paquete. Solo puede
     * cancelar el dueño de la reserva o un usuario con rol ADMIN.
     *
     * @param bookingId       la reserva a cancelar
     * @param requestingUserId el usuario que solicita la cancelación
     */
    public Booking cancelBooking(Long bookingId, Long requestingUserId) {
        Booking booking = getBookingById(bookingId);

        // Control de dueño: solo el cliente dueño de la reserva o un ADMIN
        // pueden cancelarla (evita que cualquiera cancele reservas ajenas).
        accessControlService.requireOwnerOrAdmin(requestingUserId, booking.getUser().getId());

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Booking is already cancelled");
        }

        // Corrección de bug: una reserva EXPIRED ya liberó sus cupos
        // automáticamente (ver expireStaleBookings). Si se permitiera
        // cancelarla aquí, se descontarían los cupos DOS VECES, dejando
        // bookedSlots en negativo.
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessRuleException("Cannot cancel a booking that already expired");
        }

        BookingStatus previousStatus = booking.getStatus();

        // Cambiar estado a cancelada
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Liberar cupos del paquete
        TravelPackage travelPackage = booking.getTravelPackage();
        travelPackage.setBookedSlots(
                travelPackage.getBookedSlots() - booking.getPassengerCount());

        // Si el paquete estaba agotado, vuelve a estar disponible
        if (travelPackage.getStatus() == PackageStatus.SOLD_OUT) {
            travelPackage.setStatus(PackageStatus.AVAILABLE);
        }

        packageRepository.save(travelPackage);

        // Consistencia de datos: si la reserva ya estaba pagada (CONFIRMED),
        // el pago asociado se marca como reembolsado en vez de dejarlo
        // como APPROVED, para que el seguimiento/reportes reflejen la realidad.
        if (previousStatus == BookingStatus.CONFIRMED) {
            paymentRepository.findByBookingId(bookingId).ifPresent(payment -> {
                payment.setPaymentStatus("REFUNDED");
                paymentRepository.save(payment);
            });
        }

        return booking;
    }

    /**
     * REGLA 10: EXPIRAR RESERVAS PENDIENTES AUTOMÁTICAMENTE
     * =========================================================
     * Busca reservas en estado PENDING creadas hace más de 30 minutos
     * (clientes que no completaron el pago a tiempo), las marca como
     * EXPIRED y libera los cupos que tenían apartados en el paquete.
     *
     * Este método contiene la lógica de negocio; quien lo invoca cada
     * 5 minutos es la clase ScheduledTasks (capa de scheduling, separada
     * de la capa de servicio). Queda público para poder invocarlo de
     * forma aislada desde los tests.
     *
     * @return cantidad de reservas que fueron expiradas
     */
    @Transactional
    public int expireStaleBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_EXPIRATION_MINUTES);
        List<Booking> staleBookings = bookingRepository.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING, cutoff);

        for (Booking booking : staleBookings) {
            // Marcar la reserva como expirada
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            // Liberar los cupos que esta reserva tenía apartados
            TravelPackage travelPackage = booking.getTravelPackage();
            travelPackage.setBookedSlots(
                    travelPackage.getBookedSlots() - booking.getPassengerCount());

            // Si el paquete estaba agotado, vuelve a estar disponible
            if (travelPackage.getStatus() == PackageStatus.SOLD_OUT) {
                travelPackage.setStatus(PackageStatus.AVAILABLE);
            }

            packageRepository.save(travelPackage);
        }

        return staleBookings.size();
    }
}
