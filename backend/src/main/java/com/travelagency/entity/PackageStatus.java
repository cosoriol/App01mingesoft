package com.travelagency.entity;

/**
 * ESTADOS POSIBLES DE UN PAQUETE TURÍSTICO
 * =========================================
 * Un enum (enumeración) define un conjunto fijo de valores.
 * Es como una lista cerrada: un paquete SOLO puede tener
 * uno de estos 4 estados.
 */
public enum PackageStatus {
    AVAILABLE,    // Disponible para reservar
    SOLD_OUT,     // Agotado (sin cupos)
    EXPIRED,      // No vigente (fecha ya pasó)
    CANCELLED     // Cancelado por la agencia
}
