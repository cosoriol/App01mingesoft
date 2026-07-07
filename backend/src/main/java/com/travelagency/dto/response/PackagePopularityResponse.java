package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: cuántas veces se ha reservado un paquete turístico.
 * ====================================================================
 * Épica 7: Generación de reportes ("paquetes más reservados").
 *
 * Se construye directamente desde una consulta JPQL con "constructor
 * expression" (BookingRepository.findMostBookedPackages), por eso el
 * orden y tipo de los campos del constructor debe coincidir EXACTO
 * con lo que la consulta selecciona.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackagePopularityResponse {

    private Long packageId;
    private String packageName;
    private String destination;
    private Long bookingCount;
    private Long totalPassengers;
}
