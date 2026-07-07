package com.travelagency.controller;

import com.travelagency.dto.request.CreateBookingRequest;
import com.travelagency.entity.Booking;
import com.travelagency.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * CONTROLADOR DE RESERVAS (Capa Controller)
 * ==========================================
 * Endpoints para crear, ver y cancelar reservas.
 *
 * Épicas 4 y 6: Reservas y seguimiento
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    /**
     * CREAR una nueva reserva
     * URL: POST /api/bookings?userId=1
     *
     * El userId se recibe como parámetro de la URL.
     * En producción con Keycloak, se obtendrá del token JWT.
     */
    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestParam Long userId,
            @Valid @RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    /**
     * OBTENER reservas de un usuario (Épica 6)
     * URL: GET /api/bookings/user/1
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    /**
     * OBTENER todas las reservas (admin, Épica 6)
     * URL: GET /api/bookings
     */
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    /**
     * OBTENER una reserva por ID
     * URL: GET /api/bookings/5
     */
    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    /**
     * CANCELAR una reserva
     * URL: PATCH /api/bookings/5/cancel
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}
