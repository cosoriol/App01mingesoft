package com.travelagency.dto.response;

import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de resumen de una reserva ANTES de pagar.
 * ====================================================================
 * Épica 5 (Regla 11: resumen antes de confirmar).
 *
 * El cliente debe poder ver precio original, descuentos aplicados y
 * precio final ANTES de enviar los datos de la tarjeta. Reutiliza el
 * mismo desglose de descuentos (DiscountDetail/discountSummary) que
 * ya se calculó y guardó en la Épica 4, sin volver a calcular nada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {

    private Long bookingId;
    private String packageName;
    private String destination;
    private Integer passengerCount;
    private BigDecimal baseAmount;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private List<DiscountDetail> discountDetails;
    private String discountSummary;
    private BigDecimal totalAmount;
    private BookingStatus bookingStatus;

    /**
     * Construye el resumen a partir de la reserva (sin tocar Payment,
     * porque este resumen se muestra ANTES de que exista un pago).
     */
    public static PaymentSummaryResponse from(Booking booking) {
        PaymentSummaryResponse response = new PaymentSummaryResponse();
        response.setBookingId(booking.getId());
        response.setPackageName(booking.getTravelPackage().getName());
        response.setDestination(booking.getTravelPackage().getDestination());
        response.setPassengerCount(booking.getPassengerCount());
        response.setBaseAmount(booking.getBaseAmount());
        response.setDiscountPercentage(booking.getDiscountPercentage());
        response.setDiscountAmount(booking.getDiscountAmount());
        response.setDiscountDetails(DiscountDetail.fromJson(booking.getDiscountDetails()));
        response.setDiscountSummary(booking.getDiscountSummary());
        response.setTotalAmount(booking.getTotalAmount());
        response.setBookingStatus(booking.getStatus());
        return response;
    }
}
