package com.travelagency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ENTIDAD TRAVEL PACKAGE (Tabla "travel_packages" en la BD)
 * =========================================================
 * Representa un paquete turístico que la agencia vende.
 * Ejemplo: "Aventura en Machu Picchu - 5 días"
 *
 * Épica 2: Publicación y gestión de paquetes turísticos
 */
@Entity
@Table(name = "travel_packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del paquete. Ej: "Aventura en Machu Picchu"
     */
    @Column(nullable = false)
    private String name;

    /**
     * Destino del viaje. Ej: "Cusco, Perú"
     */
    @Column(nullable = false)
    private String destination;

    /**
     * Descripción detallada del paquete.
     * @Lob = permite almacenar textos largos (más de 255 caracteres)
     */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Fecha de inicio del viaje.
     */
    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * Fecha de término del viaje.
     * Regla: debe ser posterior a startDate
     */
    @Column(nullable = false)
    private LocalDate endDate;

    /**
     * Precio por persona en dólares.
     * BigDecimal se usa para dinero (más preciso que double)
     * Regla: debe ser mayor que cero
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Cantidad total de cupos disponibles.
     * Regla: debe ser mayor que cero
     */
    @Column(nullable = false)
    private Integer totalSlots;

    /**
     * Cupos que ya fueron reservados.
     * availableSlots = totalSlots - bookedSlots
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer bookedSlots = 0;

    /**
     * Servicios incluidos en el paquete.
     * Ej: "Hotel 4 estrellas, Desayuno, Transporte, Guía turístico"
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String includedServices;

    /**
     * Restricciones o condiciones del paquete.
     * Ej: "No incluye vuelos internacionales, Seguro de viaje obligatorio"
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String restrictions;

    /**
     * Tipo de viaje.
     * Ej: "Aventura", "Playa", "Cultural", "Familiar"
     */
    private String travelType;

    /**
     * Temporada del viaje.
     * Ej: "Alta", "Baja", "Media"
     */
    private String season;

    /**
     * Estado del paquete (controla su visibilidad y disponibilidad).
     * Valores posibles: AVAILABLE, SOLD_OUT, EXPIRED, CANCELLED
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PackageStatus status = PackageStatus.AVAILABLE;

    /**
     * Duración del viaje en días (se calcula automáticamente).
     */
    private Integer durationDays;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Calcular duración automáticamente
        if (startDate != null && endDate != null) {
            durationDays = (int) (endDate.toEpochDay() - startDate.toEpochDay());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (startDate != null && endDate != null) {
            durationDays = (int) (endDate.toEpochDay() - startDate.toEpochDay());
        }
    }

    /**
     * Método auxiliar: calcula cuántos cupos quedan disponibles.
     * Ej: si hay 20 cupos totales y 5 reservados, quedan 15.
     */
    public int getAvailableSlots() {
        return totalSlots - bookedSlots;
    }
}
