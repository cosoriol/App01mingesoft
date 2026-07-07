package com.travelagency.service;

import com.travelagency.dto.request.PaymentRequest;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SERVICIO DE PAGOS (Capa Service)
 * ==================================
 * Procesa pagos simulados con tarjeta de crédito ficticia.
 *
 * Épica 5: Gestión de pagos en línea
 *
 * IMPORTANTE: Todo pago se asume como EXITOSO.
 * No hay conexión con bancos ni pasarelas reales.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    /**
     * PROCESAR UN PAGO
     * ==================
     * Proceso:
     * 1. Verificar que la reserva existe
     * 2. Verificar que no esté cancelada
     * 3. Verificar que no tenga un pago previo
     * 4. Registrar el pago (simulado, siempre exitoso)
     * 5. Cambiar el estado de la reserva a CONFIRMADA
     *
     * @param request datos de la tarjeta simulada
     * @return el pago registrado
     */
    public Payment processPayment(PaymentRequest request) {

        // PASO 1: Verificar que la reserva existe
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + request.getBookingId()));

        // PASO 2: No se puede pagar una reserva cancelada
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot pay for a cancelled booking");
        }

        // PASO 3: Solo un pago por reserva
        if (paymentRepository.existsByBookingId(request.getBookingId())) {
            throw new BusinessRuleException("Payment already exists for this booking");
        }

        // PASO 4: Obtener los últimos 4 dígitos de la tarjeta (para referencia)
        String cardLastFour = request.getCardNumber()
                .substring(request.getCardNumber().length() - 4);

        // PASO 5: Crear el registro de pago (simulado = siempre aprobado)
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .paymentMethod("CREDIT_CARD")
                .cardLastFour(cardLastFour)
                .paymentStatus("APPROVED")
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // PASO 6: Cambiar estado de la reserva a CONFIRMADA
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return savedPayment;
    }

    /**
     * OBTENER PAGO DE UNA RESERVA
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for booking: " + bookingId));
    }
}
