package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para procesar un PAGO simulado.
 * Recibe datos de tarjeta de crédito ficticia.
 *
 * IMPORTANTE: estos datos NO se validan contra ningún banco real.
 * Todo pago se asume exitoso.
 */
@Data
public class PaymentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "Expiration date is required")
    private String expirationDate;

    @NotBlank(message = "CVV is required")
    private String cvv;

    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;
}
