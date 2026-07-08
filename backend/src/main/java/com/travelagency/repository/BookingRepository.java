package com.travelagency.repository;

import com.travelagency.dto.response.BookingStatusCountResponse;
import com.travelagency.dto.response.PackagePopularityResponse;
import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import org.springframework.data.domain.Pageable;
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

    /**
     * Todas las reservas que NO están en un estado dado.
     *
     * Épica 7: se usa para analizar la efectividad de descuentos sobre
     * todas las reservas "reales" del sistema (excluyendo CANCELLED,
     * que nunca se tradujeron en un descuento realmente otorgado).
     */
    List<Booking> findByStatusNot(BookingStatus status);

    /**
     * Reservas creadas dentro de un rango de fechas, SIN filtrar por
     * estado. Épica 7: reporte detallado de ventas, cuando el admin
     * pide includeCancelled=true.
     */
    List<Booking> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Reservas creadas dentro de un rango de fechas, excluyendo los
     * estados indicados. Épica 7: comportamiento por defecto del
     * reporte de ventas (excluye CANCELLED y EXPIRED) y base del
     * ranking de paquetes vendidos (que siempre las excluye).
     */
    List<Booking> findByCreatedAtBetweenAndStatusNotIn(
            LocalDateTime start, LocalDateTime end, List<BookingStatus> excludedStatuses);

    /**
     * Ranking de paquetes por cantidad de reservas (excluye CANCELLED).
     *
     * Épica 7: reporte de "paquetes más reservados". Usa una consulta
     * JPQL con "constructor expression": en vez de devolver entidades,
     * arma directamente el DTO de respuesta desde la base de datos.
     *
     * Recibe un Pageable solo para poder limitar el resultado (ej.
     * "los primeros 10"); no se usa paginación real página a página.
     */
    @Query("SELECT new com.travelagency.dto.response.PackagePopularityResponse(" +
           "p.id, p.name, p.destination, COUNT(b), SUM(b.passengerCount)) " +
           "FROM Booking b JOIN b.travelPackage p " +
           "WHERE b.status <> 'CANCELLED' " +
           "GROUP BY p.id, p.name, p.destination " +
           "ORDER BY COUNT(b) DESC")
    List<PackagePopularityResponse> findMostBookedPackages(Pageable pageable);

    /**
     * Cuenta cuántas reservas hay en cada estado (PENDING, CONFIRMED,
     * CANCELLED, EXPIRED), considerando TODAS las reservas del sistema.
     *
     * Épica 7: reporte de "resumen de reservas por estado".
     */
    @Query("SELECT new com.travelagency.dto.response.BookingStatusCountResponse(b.status, COUNT(b)) " +
           "FROM Booking b GROUP BY b.status")
    List<BookingStatusCountResponse> countBookingsByStatus();
}
