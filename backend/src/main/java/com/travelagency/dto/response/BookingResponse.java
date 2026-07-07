package com.travelagency.dto.response;

import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de respuesta para una reserva.
 * ====================================================================
 * Épica 4 (Regla 7: transparencia de descuentos).
 *
 * El controller devuelve este DTO en vez de la entidad Booking
 * directamente, por dos razones:
 *   1. No acopla la respuesta HTTP a la estructura interna de
 *      persistencia (ej: el desglose de descuentos se guarda como
 *      JSON en una columna, pero aquí se expone ya reconstruido
 *      como una lista de objetos fácil de leer/mostrar).
 *   2. El cliente ve precio original (baseAmount), descuentos
 *      aplicados y precio final (totalAmount) ANTES de confirmar el
 *      pago, sin exponer relaciones JPA completas (User, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private String packageName;
    private String destination;
    private Integer passengerCount;
    private BigDecimal baseAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private List<DiscountDetail> discountDetails;

    /**
     * Misma información de discountDetails, pero en un solo texto
     * legible para humanos (útil para un recibo, un email, o
     * cualquier lugar donde una lista de objetos no sea práctica).
     * Ej: "Descuento por grupo (4+ pasajeros): 5.0% | Total: 5.0%
     * (tope: 20.0%)".
     */
    private String discountSummary;

    private BookingStatus status;

    /**
     * Construye el DTO de respuesta a partir de la entidad Booking.
     */
    public static BookingResponse from(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setPackageName(booking.getTravelPackage().getName());
        response.setDestination(booking.getTravelPackage().getDestination());
        response.setPassengerCount(booking.getPassengerCount());
        response.setBaseAmount(booking.getBaseAmount());
        response.setDiscountPercentage(booking.getDiscountPercentage());
        response.setDiscountAmount(booking.getDiscountAmount());
        response.setTotalAmount(booking.getTotalAmount());
        response.setDiscountDetails(DiscountDetail.fromJson(booking.getDiscountDetails()));
        response.setDiscountSummary(booking.getDiscountSummary());
        response.setStatus(booking.getStatus());
        return response;
    }
}
