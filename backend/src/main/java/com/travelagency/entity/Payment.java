package com.travelagency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ENTIDAD PAYMENT (Tabla "payments" en la BD)
 * ============================================
 * Representa un pago realizado por un cliente.
 * Cada pago está asociado a una reserva.
 *
 * Épica 5: Gestión de pagos en línea
 *
 * IMPORTANTE: El pago es SIMULADO (no se conecta a ningún
 * banco real). Todo pago se asume como exitoso.
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * La reserva que se está pagando.
     * @OneToOne = cada reserva tiene exactamente un pago
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /**
     * Monto pagado (debe ser igual al totalAmount de la reserva).
     * Regla: no se permiten pagos parciales
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Medio de pago utilizado (siempre "CREDIT_CARD" en este proyecto).
     */
    @Column(nullable = false)
    private String paymentMethod;

    /**
     * Últimos 4 dígitos de la tarjeta (para referencia).
     * Ej: "4532" (no se guarda el número completo por seguridad)
     */
    private String cardLastFour;

    /**
     * Nombre del titular de la tarjeta (dato de referencia; nunca se
     * valida contra un banco real, es un pago simulado).
     *
     * nullable = true a nivel de columna a propósito: la obligatoriedad
     * la impone la validación (@NotBlank en PaymentRequest + chequeo en
     * PaymentService), no una restricción de la BD. Esto evita romper
     * filas de pagos que ya existían antes de agregar este campo.
     */
    private String cardHolderName;

    /**
     * Estado del pago (siempre "APPROVED" porque es simulado).
     */
    @Column(nullable = false)
    @Builder.Default
    private String paymentStatus = "APPROVED";

    /**
     * Fecha y hora en que se realizó el pago.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime paymentDate;

    @PrePersist
    protected void onCreate() {
        paymentDate = LocalDateTime.now();
    }
}
