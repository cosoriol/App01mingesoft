package com.travelagency.exception;

/**
 * Excepción para reglas de negocio violadas.
 * Ej: intentar reservar más cupos de los disponibles.
 *
 * Spring devuelve un error 400 (Bad Request) al frontend.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
