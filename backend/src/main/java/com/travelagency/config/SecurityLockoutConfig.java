package com.travelagency.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CONFIGURACIÓN DE BLOQUEO DE CUENTA POR INTENTOS FALLIDOS
 * ============================================================
 * Épica 1 (Regla 6): después de N intentos fallidos consecutivos,
 * la cuenta queda bloqueada temporalmente por M minutos.
 *
 * Ejemplo (application.properties):
 *   app.security.max-failed-attempts=5
 *   app.security.lock-duration-minutes=30
 */
@Component
@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityLockoutConfig {

    /** Cantidad de intentos fallidos consecutivos que activan el bloqueo. */
    private int maxFailedAttempts = 5;

    /** Cuántos minutos dura el bloqueo una vez activado. */
    private int lockDurationMinutes = 30;
}
