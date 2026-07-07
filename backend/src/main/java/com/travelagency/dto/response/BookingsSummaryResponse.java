package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO: resumen de TODAS las reservas del sistema, agrupadas por estado.
 * Épica 7: Generación de reportes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingsSummaryResponse {

    private long totalBookings;
    private List<BookingStatusCountResponse> byStatus;
}
