package com.travelagency.entity;

/**
 * ESTADOS POSIBLES DE UNA RESERVA
 * ================================
 */
public enum BookingStatus {
    PENDING,     // Pendiente de pago
    CONFIRMED,   // Pagada y confirmada
    CANCELLED,   // Cancelada
    EXPIRED      // Expirada por falta de pago
}
