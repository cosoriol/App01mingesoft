package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO para procesar un PAGO simulado.
 * Épica 5: Gestión de pagos en línea.
 *
 * IMPORTANTE: estos datos NO se validan contra ningún banco real.
 * Las anotaciones de abajo solo validan FORMATO (Regla 9); las
 * reglas de negocio (monto correcto, un solo pago por reserva, etc.)
 * se validan en PaymentService.
 */
@Data
public class PaymentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    /**
     * Monto que el cliente confirma pagar. Se valida en el servicio
     * que sea > 0 (Regla 3) y que coincida EXACTO con el totalAmount
     * de la reserva (Regla 4) — así el servidor nunca confía a ciegas
     * en lo que el cliente cree que está pagando.
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    /**
     * Medio de pago. Regla 8: por ahora el sistema solo acepta
     * "CREDIT_CARD" (se valida en el servicio).
     */
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits")
    private String cardNumber;

    /**
     * Formato MM/YY. Que no esté vencida se valida en el servicio,
     * ya que una expresión regular no puede comparar contra "hoy".
     */
    @NotBlank(message = "Expiration date is required")
    @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Expiration date must be in MM/YY format")
    private String expirationDate;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "\\d{3}", message = "CVV must be exactly 3 digits")
    private String cvv;

    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;
}
