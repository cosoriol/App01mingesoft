package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para CREAR una reserva.
 * El cliente envía el ID del paquete y cuántos pasajeros.
 */
@Data
public class CreateBookingRequest {

    @NotNull(message = "Package ID is required")
    private Long packageId;

    @NotNull(message = "Passenger count is required")
    @Positive(message = "Passenger count must be greater than zero")
    private Integer passengerCount;
}
