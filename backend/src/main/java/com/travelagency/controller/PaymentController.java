package com.travelagency.controller;

import com.travelagency.dto.request.PaymentRequest;
import com.travelagency.dto.response.PaymentResponse;
import com.travelagency.dto.response.PaymentSummaryResponse;
import com.travelagency.entity.Payment;
import com.travelagency.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CONTROLADOR DE PAGOS (Capa Controller)
 * ========================================
 * Endpoints para procesar pagos simulados.
 *
 * Épica 5: Gestión de pagos
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * RESUMEN de la reserva ANTES de pagar (Regla 11).
     * URL: GET /api/payments/summary/5?userId=1
     * Acceso: el dueño de la reserva, o un ADMIN
     */
    @GetMapping("/summary/{bookingId}")
    public ResponseEntity<PaymentSummaryResponse> getPaymentSummary(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentSummary(bookingId, userId));
    }

    /**
     * PROCESAR un pago
     * URL: POST /api/payments?userId=1
     * Acceso: el dueño de la reserva, o un ADMIN
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestParam Long userId,
            @Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    /**
     * OBTENER pago de una reserva
     * URL: GET /api/payments/booking/5?userId=1
     * Acceso: el dueño de la reserva, o un ADMIN
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBooking(
            @PathVariable Long bookingId,
            @RequestParam Long userId) {
        return ResponseEntity.ok(PaymentResponse.from(paymentService.getPaymentByBookingId(bookingId, userId)));
    }
}
