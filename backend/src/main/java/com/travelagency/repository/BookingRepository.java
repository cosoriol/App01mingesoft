package com.travelagency.repository;

import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REPOSITORIO DE RESERVAS (Capa Repository)
 * ==========================================
 * Conecta con la tabla "bookings" en la BD.
 *
 * Épicas 4, 6 y 7: Reservas, seguimiento y reportes
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Busca todas las reservas de un usuario específico.
     * Épica 6: el cliente solo ve SUS reservas.
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Busca reservas de un usuario por estado.
     * Ej: ver solo las reservas confirmadas de un cliente
     */
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    /**
     * Cuenta cuántas reservas CONFIRMADAS tiene un usuario.
     * Se usa para determinar si es "cliente frecuente" (≥ 3 reservas pagadas).
     *
     * Épica 4: Descuento por cliente frecuente
     */
    long countByUserIdAndStatus(Long userId, BookingStatus status);

    /**
     * Busca reservas dentro de un rango de fechas.
     * Se usa para los reportes (Épica 7).
     *
     * Excluye reservas canceladas por defecto.
     */
    @Query("SELECT b FROM Booking b WHERE b.createdAt BETWEEN :startDate AND :endDate " +
           "AND b.status <> 'CANCELLED' ORDER BY b.createdAt DESC")
    List<Booking> findBookingsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Cuenta reservas de un usuario en un período (para descuento multi-paquete).
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId " +
           "AND b.createdAt >= :since AND b.status <> 'CANCELLED'")
    long countRecentBookingsByUser(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    /**
     * Busca reservas en un estado específico creadas antes de una fecha límite.
     *
     * Épica 4 (Regla 10): usado para encontrar reservas PENDING vencidas
     * (creadas hace más de 30 minutos) y expirarlas automáticamente.
     */
    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime cutoff);
}
