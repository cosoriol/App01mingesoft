package com.travelagency.dto.response;

import com.travelagency.entity.Booking;
import com.travelagency.entity.Payment;
import com.travelagency.entity.TravelPackage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta tras procesar un pago.
 * ====================================================================
 * Épica 5 (Regla 12: confirmación clara).
 *
 * Se devuelve en vez de la entidad Payment directamente para no
 * exponer la relación JPA completa hacia Booking/User, y para incluir
 * un mensaje de confirmación explícito junto con los datos del viaje
 * que se acaba de pagar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount;
    private String paymentMethod;
    private String cardLastFour;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String message;

    // Datos de la reserva pagada, para un resumen completo en la confirmación
    private String packageName;
    private String destination;
    private Integer passengerCount;
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Construye el DTO a partir de la entidad Payment (y su Booking asociado).
     */
    public static PaymentResponse from(Payment payment) {
        Booking booking = payment.getBooking();
        TravelPackage travelPackage = booking.getTravelPackage();

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getId());
        response.setBookingId(booking.getId());
        response.setAmount(payment.getAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setCardLastFour(payment.getCardLastFour());
        response.setPaymentStatus(payment.getPaymentStatus());
        response.setPaymentDate(payment.getPaymentDate());
        response.setMessage("Pago procesado exitosamente");
        response.setPackageName(travelPackage.getName());
        response.setDestination(travelPackage.getDestination());
        response.setPassengerCount(booking.getPassengerCount());
        response.setStartDate(travelPackage.getStartDate());
        response.setEndDate(travelPackage.getEndDate());
        return response;
    }
}
