package com.travelagency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ENTIDAD PROMOTION (Tabla "promotions" en la BD)
 * ==================================================
 * Épica 4 (Regla 5): promociones por tiempo limitado.
 *
 * Representa una campaña de descuento temporal (ej: "Verano 2026")
 * que se suma automáticamente a los demás descuentos de una reserva
 * mientras esté activa Y la fecha de la reserva esté dentro de su
 * rango de vigencia [startDate, endDate].
 */
@Entity
@Table(name = "promotions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre de la promoción. Ej: "Verano 2026". */
    @Column(nullable = false)
    private String name;

    /** Descripción de la promoción (opcional). */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Porcentaje de descuento que otorga esta promoción. */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /** Fecha desde la cual la promoción está vigente (inclusive). */
    @Column(nullable = false)
    private LocalDate startDate;

    /** Fecha hasta la cual la promoción está vigente (inclusive). */
    @Column(nullable = false)
    private LocalDate endDate;

    /**
     * Interruptor manual: permite desactivar la promoción antes de
     * tiempo sin tener que borrarla ni esperar a que venza endDate.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
