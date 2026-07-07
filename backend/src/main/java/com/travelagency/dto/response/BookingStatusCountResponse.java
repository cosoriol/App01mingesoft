package com.travelagency.dto.response;

import com.travelagency.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: cuántas reservas hay en un estado determinado.
 * Épica 7: Generación de reportes ("resumen de reservas por estado").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusCountResponse {

    private BookingStatus status;
    private Long count;
}
