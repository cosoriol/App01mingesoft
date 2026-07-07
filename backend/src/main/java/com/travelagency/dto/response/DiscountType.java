package com.travelagency.dto.response;

/**
 * TIPOS DE DESCUENTO (Épica 4: Reglas comerciales de descuentos)
 * ==================================================================
 * Identifica de dónde viene cada ítem del desglose de descuentos de
 * una reserva (Regla 7: transparencia). Permite que el frontend
 * distinga y estilice cada tipo sin tener que parsear texto.
 */
public enum DiscountType {
    /** Regla 1: descuento por cantidad de pasajeros. */
    GROUP,
    /** Regla 2: descuento por cliente frecuente. */
    FREQUENT_CLIENT,
    /** Regla 3: descuento por compra de múltiples paquetes. */
    MULTI_PACKAGE,
    /** Regla 5: promoción por tiempo limitado vigente. */
    PROMOTION
}
