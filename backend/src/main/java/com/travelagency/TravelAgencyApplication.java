package com.travelagency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de la aplicación TravelAgency.
 *
 * @SpringBootApplication es la anotación que le dice a Spring Boot
 * que esta es la clase de inicio. Combina tres anotaciones:
 *   - @Configuration: indica que es una clase de configuración
 *   - @EnableAutoConfiguration: activa la configuración automática
 *   - @ComponentScan: busca automáticamente todas las clases del proyecto
 *
 * @EnableScheduling activa las tareas programadas con @Scheduled
 * (usado por ScheduledTasks para expirar reservas PENDING vencidas).
 */
@SpringBootApplication
@EnableScheduling
public class TravelAgencyApplication {

    public static void main(String[] args) {
        // Este método inicia toda la aplicación Spring Boot
        SpringApplication.run(TravelAgencyApplication.class, args);
    }
}
