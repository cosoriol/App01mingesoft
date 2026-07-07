package com.travelagency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ENTIDAD BOOKING (Tabla "bookings" en la BD)
 * ============================================
 * Representa una reserva hecha por un cliente.
 * Conecta un usuario con un paquete turístico.
 *
 * Épica 4: Proceso de reserva en línea
 */
@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * El cliente que hizo la reserva.
     * @ManyToOne = muchas reservas pueden pertenecer a un mismo usuario
     * @JoinColumn = la columna "user_id" en la tabla bookings
     *               referencia al id de la tabla users
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * El paquete turístico reservado.
     * @ManyToOne = muchas reservas pueden ser del mismo paquete
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private TravelPackage travelPackage;

    /**
     * Cantidad de pasajeros en esta reserva.
     * Regla: debe ser mayor que cero
     * Regla: no puede exceder los cupos disponibles del paquete
     */
    @Column(nullable = false)
    private Integer passengerCount;

    /**
     * Monto base (precio × pasajeros, SIN descuentos).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    /**
     * Porcentaje total de descuento aplicado.
     * Ej: 15.00 significa 15% de descuento
     * Regla: máximo 20%
     */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    /**
     * Monto del descuento en dinero.
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Monto final a pagar (baseAmount - discountAmount).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Detalle de los descuentos aplicados.
     * Ej: "Descuento por grupo: 5%, Cliente frecuente: 10%"
     * Regla de transparencia: el cliente debe ver qué descuentos se aplicaron
     */
    @Column(columnDefinition = "TEXT")
    private String discountDetails;

    /**
     * Estado de la reserva.
     * Valores: PENDING, CONFIRMED, CANCELLED, EXPIRED
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
