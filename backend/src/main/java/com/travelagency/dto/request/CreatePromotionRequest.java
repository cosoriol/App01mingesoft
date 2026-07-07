package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para CREAR una promoción por tiempo limitado.
 * Épica 4 (Regla 5): promociones temporales de descuento.
 */
@Data
public class CreatePromotionRequest {

    @NotBlank(message = "Promotion name is required")
    private String name;

    private String description;

    @NotNull(message = "Discount percentage is required")
    @Positive(message = "Discount percentage must be greater than zero")
    private BigDecimal discountPercentage;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
