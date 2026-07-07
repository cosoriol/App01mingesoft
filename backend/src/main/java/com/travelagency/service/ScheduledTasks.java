package com.travelagency.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TAREAS PROGRAMADAS (Scheduled Tasks)
 * =======================================
 * Ejecuta procesos automáticos en segundo plano, sin que un usuario
 * tenga que hacer una petición HTTP para dispararlos.
 *
 * Esta clase SOLO dispara la tarea (capa de scheduling); la lógica
 * de negocio real vive en BookingService, respetando la arquitectura
 * por capas del proyecto.
 *
 * Épica 4 (Regla 10): expiración automática de reservas PENDING.
 */
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final BookingService bookingService;

    /**
     * Cada 5 minutos revisa si hay reservas PENDING vencidas
     * (creadas hace más de 30 minutos) y las expira liberando
     * los cupos que tenían reservados.
     *
     * fixedRate = 5 minutos, expresado en milisegundos (5 * 60 * 1000).
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void expirePendingBookings() {
        bookingService.expireStaleBookings();
    }
}
