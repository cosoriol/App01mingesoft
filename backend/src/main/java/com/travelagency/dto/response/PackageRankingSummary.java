package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO: totales del ranking de paquetes vendidos por período (Épica 7).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageRankingSummary {

    private int totalPackagesWithSales;
    private long totalBookings;
    private long totalPassengers;
    private BigDecimal totalAmount;
}
