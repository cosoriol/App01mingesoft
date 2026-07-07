package com.travelagency.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * CONFIGURACIÓN DE REGLAS COMERCIALES DE DESCUENTOS
 * ====================================================
 * Épica 4: Reglas comerciales de descuentos del sistema de reservas.
 *
 * Agrupa TODOS los valores configurables de las reglas de descuento
 * (umbrales y porcentajes) para que se puedan ajustar desde
 * application.properties SIN tocar código ni recompilar.
 *
 * Ejemplo (application.properties):
 *   app.discount.group.threshold=4
 *   app.discount.group.percentage=5.0
 *   app.discount.frequent.threshold=3
 *   app.discount.frequent.percentage=10.0
 *   app.discount.multipackage.days=30
 *   app.discount.multipackage.percentage=3.0
 *   app.discount.max.percentage=20.0
 *
 * @ConfigurationProperties enlaza cada propiedad con el campo
 * correspondiente por convención de nombres; si una propiedad no
 * está definida, se usa el valor por defecto del campo.
 */
@Component
@ConfigurationProperties(prefix = "app.discount")
@Data
public class DiscountConfig {

    /** REGLA 1: descuento por cantidad de pasajeros (grupo). */
    private Group group = new Group();

    /** REGLA 2: descuento por cliente frecuente. */
    private Frequent frequent = new Frequent();

    /** REGLA 3: descuento por compra de múltiples paquetes. */
    private MultiPackage multipackage = new MultiPackage();

    /** REGLA 4: tope máximo de descuento acumulado. */
    private Max max = new Max();

    @Data
    public static class Group {
        /** Mínimo de pasajeros para aplicar el descuento por grupo. */
        private int threshold = 4;
        /** Porcentaje de descuento por grupo. */
        private BigDecimal percentage = new BigDecimal("5.0");
    }

    @Data
    public static class Frequent {
        /** Mínimo de reservas CONFIRMED para considerar "cliente frecuente". */
        private int threshold = 3;
        /** Porcentaje de descuento para cliente frecuente. */
        private BigDecimal percentage = new BigDecimal("10.0");
    }

    @Data
    public static class MultiPackage {
        /** Días hacia atrás para buscar otra reserva no cancelada del cliente. */
        private int days = 30;
        /** Porcentaje de descuento por multi-paquete. */
        private BigDecimal percentage = new BigDecimal("3.0");
    }

    @Data
    public static class Max {
        /** Tope máximo de descuento acumulado (todas las reglas sumadas). */
        private BigDecimal percentage = new BigDecimal("20.0");
    }
}
