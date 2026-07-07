package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para CREAR un paquete turístico (lo que envía el frontend).
 *
 * DTO = Data Transfer Object
 * Es un objeto que transporta datos entre el frontend y el backend.
 * No se guarda en la BD, solo sirve para recibir/enviar información.
 *
 * Las anotaciones @NotBlank, @NotNull, @Positive validan
 * automáticamente que los datos sean correctos antes de procesarlos.
 */
@Data
public class CreatePackageRequest {

    @NotBlank(message = "Package name is required")
    private String name;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Total slots is required")
    @Positive(message = "Total slots must be greater than zero")
    private Integer totalSlots;

    private String includedServices;
    private String restrictions;
    private String travelType;
    private String season;
}
