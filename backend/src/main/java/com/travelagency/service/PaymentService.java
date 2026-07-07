package com.travelagency.service;

import com.travelagency.dto.request.PaymentRequest;
import com.travelagency.dto.response.PaymentSummaryResponse;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.regex.Pattern;

/**
 * SERVICIO DE PAGOS (Capa Service)
 * ==================================
 * Procesa pagos simulados con tarjeta de crédito ficticia.
 *
 * Épica 5: Gestión de pagos en línea
 *
 * IMPORTANTE: El pago es SIMULADO. No hay conexión con bancos ni
 * pasarelas reales: no se simulan rechazos por fondos insuficientes,
 * tarjeta robada, etc. Todo pago que pasa las validaciones de
 * FORMATO (Regla 9) queda APPROVED (Regla 10).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    /** Regla 9: número de tarjeta = exactamente 16 dígitos numéricos. */
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\d{16}");

    /** Regla 9: CVV = exactamente 3 dígitos numéricos. */
    private static final Pattern CVV_PATTERN = Pattern.compile("\\d{3}");

    /** Regla 9: fecha de expiración = formato MM/YY. */
    private static final Pattern EXPIRATION_PATTERN = Pattern.compile("(0[1-9]|1[0-2])/\\d{2}");

    /** Regla 8: único medio de pago aceptado en este proyecto. */
    private static final String SUPPORTED_PAYMENT_METHOD = "CREDIT_CARD";

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final AccessControlService accessControlService;

    /**
     * PROCESAR UN PAGO
     * ==================
     * Implementa las 12 reglas de la Épica 5:
     *
     * REGLA 1: la reserva debe existir
     * (control de acceso: solo el dueño de la reserva, o un ADMIN, puede pagarla)
     * REGLA 2: no se paga una reserva CANCELLED o EXPIRED
     * REGLA 5: solo un pago por reserva
     * REGLA 8: solo se acepta paymentMethod = CREDIT_CARD
     * REGLA 9: validaciones de formato de tarjeta (número, CVV, fecha, titular)
     * REGLA 3: el monto debe ser mayor que cero
     * REGLA 4: el monto debe coincidir EXACTO con el total de la reserva
     * REGLA 7: se guardan los datos de la transacción (sin el número completo)
     * REGLA 10: todo pago que pasa las validaciones queda APPROVED
     * REGLA 6: la reserva pasa a CONFIRMED, en la misma transacción
     *
     * @param requestingUserId quien está pagando (debe ser el dueño de la reserva, o un ADMIN)
     * @param request          datos de la tarjeta simulada
     * @return el pago registrado
     */
    public Payment processPayment(Long requestingUserId, PaymentRequest request) {

        // REGLA 1: la reserva debe existir.
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + request.getBookingId()));

        // Control de acceso: solo el dueño de la reserva (o un ADMIN)
        // puede pagarla. Corrección de bug: antes cualquiera con los
        // datos de una tarjeta simulada podía pagar —y así confirmar—
        // la reserva de otra persona.
        accessControlService.requireOwnerOrAdmin(requestingUserId, booking.getUser().getId());

        // REGLA 2: no se puede pagar una reserva cancelada o ya expirada.
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot pay for a cancelled booking");
        }
        if (booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BusinessRuleException("Cannot pay for an expired booking");
        }

        // REGLA 5: solo se permite un pago por reserva (se valida antes
        // de pedirle al "cliente" datos de tarjeta innecesariamente).
        if (paymentRepository.existsByBookingId(request.getBookingId())) {
            throw new BusinessRuleException("Payment already exists for this booking");
        }

        // REGLA 8: solo se aceptan tarjetas de crédito simuladas.
        if (!SUPPORTED_PAYMENT_METHOD.equals(request.getPaymentMethod())) {
            throw new BusinessRuleException(
                    "Unsupported payment method: " + request.getPaymentMethod()
                    + ". Only " + SUPPORTED_PAYMENT_METHOD + " is accepted");
        }

        // REGLA 9: validar el FORMATO de los datos de la tarjeta
        // (nunca contra un banco real, solo forma).
        validateCardFormat(request);

        // REGLA 3: el monto a pagar debe ser mayor que cero.
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Payment amount must be greater than zero");
        }

        // REGLA 4: no se permiten pagos parciales ni cuotas; el monto
        // debe ser EXACTAMENTE el total de la reserva.
        if (request.getAmount().compareTo(booking.getTotalAmount()) != 0) {
            throw new BusinessRuleException(
                    "Payment amount (" + request.getAmount()
                    + ") does not match the booking total (" + booking.getTotalAmount() + ")");
        }

        // REGLA 7: se guardan solo los últimos 4 dígitos, NUNCA el
        // número completo de la tarjeta.
        String cardLastFour = request.getCardNumber()
                .substring(request.getCardNumber().length() - 4);

        // REGLA 7 y 10: se registra el pago. Al pasar las validaciones
        // de formato, el pago SIEMPRE queda APPROVED (no se simulan
        // rechazos bancarios).
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .cardLastFour(cardLastFour)
                .cardHolderName(request.getCardHolderName())
                .paymentStatus("APPROVED")
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // REGLA 6: la reserva pasa a CONFIRMED en la MISMA transacción
        // (toda la clase está anotada @Transactional).
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return savedPayment;
    }

    /**
     * REGLA 9: valida el FORMATO de los datos de la tarjeta.
     *
     * Se revalida aquí (además de las anotaciones @Pattern del DTO)
     * para que la regla se cumpla incluso si processPayment() se
     * invoca directamente sin pasar por el controller (por ejemplo,
     * desde los tests unitarios).
     */
    private void validateCardFormat(PaymentRequest request) {
        if (!CARD_NUMBER_PATTERN.matcher(request.getCardNumber()).matches()) {
            throw new BusinessRuleException("Card number must be exactly 16 digits");
        }
        if (!CVV_PATTERN.matcher(request.getCvv()).matches()) {
            throw new BusinessRuleException("CVV must be exactly 3 digits");
        }
        if (!EXPIRATION_PATTERN.matcher(request.getExpirationDate()).matches()) {
            throw new BusinessRuleException("Expiration date must be in MM/YY format");
        }
        if (request.getCardHolderName() == null || request.getCardHolderName().isBlank()) {
            throw new BusinessRuleException("Card holder name is required");
        }

        // La fecha de expiración no puede ser una fecha ya pasada.
        // "YY" se interpreta como 20YY (proyecto ambientado en la década 2020-2030).
        String[] parts = request.getExpirationDate().split("/");
        YearMonth expiration = YearMonth.of(2000 + Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
        if (expiration.isBefore(YearMonth.now())) {
            throw new BusinessRuleException("Card expiration date has already passed");
        }
    }

    /**
     * REGLA 11: resumen de la reserva ANTES de pagar.
     * Muestra precio original, descuentos aplicados y precio final,
     * para que el cliente confirme con información completa antes de
     * ingresar los datos de la tarjeta.
     *
     * Acceso: el dueño de la reserva, o un ADMIN (corrección de bug:
     * antes cualquiera podía ver el monto a pagar de cualquier reserva).
     */
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary(Long bookingId, Long requestingUserId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + bookingId));
        accessControlService.requireOwnerOrAdmin(requestingUserId, booking.getUser().getId());
        return PaymentSummaryResponse.from(booking);
    }

    /**
     * OBTENER PAGO DE UNA RESERVA.
     *
     * Acceso: el dueño de la reserva, o un ADMIN (corrección de bug:
     * antes cualquiera podía ver el monto pagado y los últimos 4
     * dígitos de la tarjeta de cualquier reserva).
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId, Long requestingUserId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for booking: " + bookingId));
        accessControlService.requireOwnerOrAdmin(requestingUserId, payment.getBooking().getUser().getId());
        return payment;
    }
}
