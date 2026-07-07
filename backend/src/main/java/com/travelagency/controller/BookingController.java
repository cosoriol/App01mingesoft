package com.travelagency.controller;

import com.travelagency.dto.request.CreateBookingRequest;
import com.travelagency.dto.response.BookingResponse;
import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
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
 *
 * Épica 4 (Regla 7): todos los endpoints devuelven BookingResponse
 * (no la entidad Booking directamente), para que el cliente vea el
 * precio original, los descuentos aplicados y el precio final de
 * forma clara, sin exponer relaciones internas de persistencia.
 *
 * Todos los endpoints de lectura reciben un "requesterId"/"userId"
 * para que BookingService pueda validar que quien pregunta tiene
 * permiso de ver esos datos (dueño o ADMIN) — ver AccessControlService.
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
    public ResponseEntity<BookingResponse> createBooking(
            @RequestParam Long userId,
            @Valid @RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking));
    }

    /**
     * OBTENER reservas de un usuario (Épica 6)
     * URL: GET /api/bookings/user/1?requesterId=1
     * URL: GET /api/bookings/user/1?requesterId=1&status=CONFIRMED
     *
     * requesterId = quien pregunta; debe ser el mismo usuario 1, o un ADMIN.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookings(
            @PathVariable Long userId,
            @RequestParam Long requesterId,
            @RequestParam(required = false) BookingStatus status) {
        List<Booking> bookings = status != null
                ? bookingService.getBookingsByUserAndStatus(userId, status, requesterId)
                : bookingService.getBookingsByUser(userId, requesterId);
        return ResponseEntity.ok(bookings.stream().map(BookingResponse::from).toList());
    }

    /**
     * OBTENER todas las reservas (admin, Épica 6)
     * URL: GET /api/bookings?userId=3   (userId = admin que consulta)
     * Acceso: solo ADMIN
     */
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings(@RequestParam Long userId) {
        return ResponseEntity.ok(
                bookingService.getAllBookings(userId).stream().map(BookingResponse::from).toList());
    }

    /**
     * OBTENER una reserva por ID
     * URL: GET /api/bookings/5?userId=1
     * Acceso: el dueño de la reserva, o un ADMIN
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            @RequestParam Long userId) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.getBookingByIdForRequester(id, userId)));
    }

    /**
     * CANCELAR una reserva
     * URL: PATCH /api/bookings/5/cancel?userId=1
     *
     * El userId identifica a quien solicita la cancelación, para que
     * el servicio valide que sea el dueño de la reserva (o un ADMIN).
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @RequestParam Long userId) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.cancelBooking(id, userId)));
    }
}
