package com.travelagency.service;

import com.travelagency.dto.request.CreateBookingRequest;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.TravelPackageRepository;
import com.travelagency.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICIO DE RESERVAS (Capa Service)
 * =====================================
 * Contiene la lógica de negocio más compleja del sistema:
 * crear reservas, calcular descuentos, validar cupos.
 *
 * Épica 4: Proceso de reserva en línea
 *
 * ESTA ES LA CLASE MÁS IMPORTANTE DEL PROYECTO.
 * Aquí se implementan las reglas de descuentos y validaciones.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TravelPackageRepository packageRepository;
    private final UserRepository userRepository;

    // ============================================================
    // CONSTANTES DE CONFIGURACIÓN DE DESCUENTOS
    // Estas son las reglas de negocio para los descuentos
    // ============================================================

    /** Mínimo de pasajeros para descuento por grupo */
    private static final int GROUP_DISCOUNT_THRESHOLD = 4;

    /** Porcentaje de descuento por grupo (5%) */
    private static final BigDecimal GROUP_DISCOUNT_PERCENT = new BigDecimal("5.00");

    /** Mínimo de reservas confirmadas para ser "cliente frecuente" */
    private static final int FREQUENT_CLIENT_THRESHOLD = 3;

    /** Porcentaje de descuento para cliente frecuente (10%) */
    private static final BigDecimal FREQUENT_DISCOUNT_PERCENT = new BigDecimal("10.00");

    /** Mínimo de reservas recientes para descuento multi-paquete */
    private static final int MULTI_PACKAGE_THRESHOLD = 1;

    /** Porcentaje de descuento por múltiples paquetes (3%) */
    private static final BigDecimal MULTI_PACKAGE_DISCOUNT_PERCENT = new BigDecimal("3.00");

    /** Máximo de descuento acumulable (20%) */
    private static final BigDecimal MAX_DISCOUNT_PERCENT = new BigDecimal("20.00");

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

        // REGLA 7: monto base = precio por persona × cantidad de pasajeros.
        BigDecimal pricePerPerson = travelPackage.getPrice();
        BigDecimal baseAmount = pricePerPerson.multiply(
                BigDecimal.valueOf(request.getPassengerCount()));

        // REGLA 7: calcular descuentos aplicables (grupo, cliente frecuente, multi-paquete).
        DiscountResult discountResult = calculateDiscounts(
                userId, request.getPassengerCount(), baseAmount);

        // REGLA 7: monto final = monto base - descuentos (nunca negativo).
        BigDecimal totalAmount = baseAmount.subtract(discountResult.discountAmount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // REGLA 6: el id se genera automáticamente al guardar (@GeneratedValue en Booking).
        // REGLA 8: toda reserva nueva comienza en estado PENDING.
        Booking booking = Booking.builder()
                .user(user)
                .travelPackage(travelPackage)
                .passengerCount(request.getPassengerCount())
                .baseAmount(baseAmount)
                .discountPercentage(discountResult.totalPercentage)
                .discountAmount(discountResult.discountAmount)
                .totalAmount(totalAmount)
                .discountDetails(discountResult.details)
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
     * CALCULAR DESCUENTOS APLICABLES
     * ================================
     * Evalúa todas las reglas de descuento y determina cuáles aplican.
     *
     * Tipos de descuento:
     * 1. Por grupo (≥ 4 pasajeros): 5%
     * 2. Por cliente frecuente (≥ 3 reservas pagadas): 10%
     * 3. Por multi-paquete (otra reserva en los últimos 30 días): 3%
     *
     * Regla: los descuentos son ACUMULABLES pero con un TOPE de 20%
     */
    private DiscountResult calculateDiscounts(Long userId, int passengerCount, BigDecimal baseAmount) {

        BigDecimal totalPercentage = BigDecimal.ZERO;
        List<String> discountDescriptions = new ArrayList<>();

        // --- Descuento 1: Por grupo ---
        // Si viajan 4 o más personas, se aplica 5% de descuento
        if (passengerCount >= GROUP_DISCOUNT_THRESHOLD) {
            totalPercentage = totalPercentage.add(GROUP_DISCOUNT_PERCENT);
            discountDescriptions.add("Group discount (" + GROUP_DISCOUNT_THRESHOLD
                    + "+ passengers): " + GROUP_DISCOUNT_PERCENT + "%");
        }

        // --- Descuento 2: Cliente frecuente ---
        // Si el cliente tiene 3 o más reservas confirmadas (pagadas)
        long confirmedBookings = bookingRepository.countByUserIdAndStatus(
                userId, BookingStatus.CONFIRMED);
        if (confirmedBookings >= FREQUENT_CLIENT_THRESHOLD) {
            totalPercentage = totalPercentage.add(FREQUENT_DISCOUNT_PERCENT);
            discountDescriptions.add("Frequent client discount ("
                    + confirmedBookings + " confirmed bookings): "
                    + FREQUENT_DISCOUNT_PERCENT + "%");
        }

        // --- Descuento 3: Múltiples paquetes ---
        // Si el cliente hizo otra reserva en los últimos 30 días
        long recentBookings = bookingRepository.countRecentBookingsByUser(
                userId, LocalDateTime.now().minusDays(30));
        if (recentBookings >= MULTI_PACKAGE_THRESHOLD) {
            totalPercentage = totalPercentage.add(MULTI_PACKAGE_DISCOUNT_PERCENT);
            discountDescriptions.add("Multi-package discount: "
                    + MULTI_PACKAGE_DISCOUNT_PERCENT + "%");
        }

        // --- Aplicar tope máximo de 20% ---
        if (totalPercentage.compareTo(MAX_DISCOUNT_PERCENT) > 0) {
            totalPercentage = MAX_DISCOUNT_PERCENT;
            discountDescriptions.add("(Capped at maximum " + MAX_DISCOUNT_PERCENT + "%)");
        }

        // Calcular el monto del descuento en dinero
        BigDecimal discountAmount = baseAmount
                .multiply(totalPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        String details = discountDescriptions.isEmpty()
                ? "No discounts applied"
                : String.join(" | ", discountDescriptions);

        return new DiscountResult(totalPercentage, discountAmount, details);
    }

    /**
     * Clase interna para devolver el resultado de los descuentos.
     */
    private record DiscountResult(
            BigDecimal totalPercentage,
            BigDecimal discountAmount,
            String details) {}

    /**
     * OBTENER RESERVAS DE UN USUARIO (Épica 6)
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    /**
     * OBTENER TODAS LAS RESERVAS (para administradores, Épica 6)
     */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * OBTENER UNA RESERVA POR ID
     */
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + id));
    }

    /**
     * CANCELAR UNA RESERVA
     * Al cancelar, se liberan los cupos del paquete.
     */
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Booking is already cancelled");
        }

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
