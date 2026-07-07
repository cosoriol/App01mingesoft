package com.travelagency.dto.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO: un ítem individual del desglose de descuentos de una reserva.
 * ====================================================================
 * Épica 4 (Regla 7: transparencia de descuentos).
 *
 * DiscountService arma una lista de estos objetos por cada reserva
 * (uno por cada descuento que aplicó: grupo, cliente frecuente,
 * multi-paquete, promociones). Esa lista se guarda como JSON en
 * Booking.discountDetails, de forma que se pueda reconstruir
 * exactamente igual más adelante (en BookingResponse) sin tener que
 * volver a calcular los descuentos, que podrían cambiar con el
 * tiempo (ej: el cliente deja de ser "frecuente", una promoción vence).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountDetail {

    private static final ObjectMapper JSON = new ObjectMapper();

    private DiscountType type;
    private String description;
    private BigDecimal percentage;

    /**
     * Convierte el desglose completo a JSON para persistirlo en
     * Booking.discountDetails.
     */
    public static String toJson(List<DiscountDetail> details) {
        try {
            return JSON.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize discount breakdown", e);
        }
    }

    /**
     * Reconstruye el desglose estructurado desde el JSON guardado en
     * la base de datos. Si el valor es nulo, vacío o corrupto,
     * devuelve una lista vacía en vez de lanzar una excepción (una
     * reserva sin desglose legible no debería impedir mostrarla).
     */
    public static List<DiscountDetail> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JSON.readValue(json, new TypeReference<List<DiscountDetail>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Arma la versión en texto plano y legible del desglose, para
     * mostrar en un recibo, un email o cualquier lugar donde una
     * lista de objetos no sea práctica.
     *
     * Ejemplo de salida:
     *   "Descuento por grupo (4+ pasajeros): 5.0% | Cliente frecuente
     *    (5 reservas confirmadas): 10.0% | Total: 15.0% (tope: 20.0%)"
     */
    public static String toSummary(List<DiscountDetail> details, BigDecimal totalPercentage, BigDecimal maxPercentage) {
        if (details.isEmpty()) {
            return "No se aplicaron descuentos";
        }

        String items = details.stream()
                .map(d -> d.getDescription() + ": " + d.getPercentage() + "%")
                .collect(Collectors.joining(" | "));

        return items + " | Total: " + totalPercentage + "% (tope: " + maxPercentage + "%)";
    }
}
