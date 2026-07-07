package com.travelagency.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CONFIGURACIÓN DE JACKSON PARA ENTIDADES DE HIBERNATE
 * =======================================================
 * Este proyecto devuelve entidades JPA directamente como JSON
 * (sin DTOs de respuesta), lo cual es simple pero tiene un problema:
 * las relaciones @ManyToOne(fetch = LAZY), como Booking.user o
 * Booking.travelPackage, llegan como "proxies" de Hibernate hasta
 * que algo los usa. Si Jackson intenta convertir a JSON un proxy
 * que nunca se inicializó, falla con un error 500.
 *
 * Hibernate6Module le enseña a Jackson a inicializar esos proxies
 * automáticamente antes de serializarlos, para que endpoints como
 * "obtener mis reservas" siempre incluyan los datos completos del
 * paquete y del usuario asociado.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        Hibernate6Module module = new Hibernate6Module();
        // Sin esto, un proxy no inicializado se convertiría en "null"
        // en el JSON en vez de cargarse; preferimos que SIEMPRE
        // se cargue el dato real (ej: nombre del paquete reservado).
        module.enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        return module;
    }
}
