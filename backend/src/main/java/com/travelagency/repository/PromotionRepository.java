package com.travelagency.repository;

import com.travelagency.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * REPOSITORIO DE PROMOCIONES (Capa Repository)
 * ===============================================
 * Épica 4 (Regla 5): promociones por tiempo limitado.
 */
@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /**
     * Busca promociones activas cuya fecha de vigencia incluya la
     * fecha indicada (normalmente, el día en que se crea una reserva).
     */
    @Query("SELECT p FROM Promotion p WHERE p.active = true "
            + "AND :date BETWEEN p.startDate AND p.endDate")
    List<Promotion> findActivePromotions(@Param("date") LocalDate date);
}
