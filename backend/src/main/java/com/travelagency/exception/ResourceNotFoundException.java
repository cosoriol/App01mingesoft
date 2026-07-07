package com.travelagency.exception;

/**
 * Excepción que se lanza cuando no se encuentra un recurso.
 * Ej: buscar un paquete con ID 999 que no existe.
 *
 * Spring automáticamente devuelve un error 404 al frontend.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
