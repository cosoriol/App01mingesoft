package com.travelagency.controller;

import com.travelagency.dto.request.PaymentRequest;
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
     * PROCESAR un pago
     * URL: POST /api/payments
     */
    @PostMapping
    public ResponseEntity<Payment> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    /**
     * OBTENER pago de una reserva
     * URL: GET /api/payments/booking/5
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> getPaymentByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }
}
