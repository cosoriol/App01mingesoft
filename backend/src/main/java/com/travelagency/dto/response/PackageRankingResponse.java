package com.travelagency.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO: ranking completo de paquetes vendidos por período (Épica 7).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackageRankingResponse {

    private List<PackageRankingItem> ranking;
    private PackageRankingSummary summary;
}
