package com.travelagency.repository;

import com.travelagency.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Pagos realizados dentro de un rango de fechas.
     * Épica 7: para incluir en el reporte de ventas las reservas cuya
     * fecha de PAGO cae en el período, aunque su fecha de creación
     * haya sido anterior al inicio del rango.
     */
    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Pagos asociados a un conjunto de reservas.
     * Épica 7: permite enriquecer cada fila de un reporte con su
     * monto pagado en una sola consulta, en vez de una por reserva.
     */
    List<Payment> findByBookingIdIn(List<Long> bookingIds);
}
