package com.travelagency.repository;

import com.travelagency.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * REPOSITORIO DE PAGOS (Capa Repository)
 * ========================================
 * Conecta con la tabla "payments" en la BD.
 *
 * Épica 5: Gestión de pagos
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Busca el pago asociado a una reserva.
     */
    Optional<Payment> findByBookingId(Long bookingId);

    /**
     * Verifica si una reserva ya tiene un pago registrado.
     * Regla: solo se permite UN pago por reserva.
     */
    boolean existsByBookingId(Long bookingId);
}
