package com.travelagency.service;

import com.travelagency.dto.request.CreatePromotionRequest;
import com.travelagency.entity.Promotion;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SERVICIO DE PROMOCIONES (Capa Service)
 * =========================================
 * Épica 4 (Regla 5): promociones por tiempo limitado.
 *
 * CRUD básico de promociones. El cálculo de si una promoción aplica
 * a una reserva en particular NO vive aquí: eso lo hace DiscountService,
 * que consulta PromotionRepository directamente al calcular descuentos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final AccessControlService accessControlService;

    /**
     * CREAR una nueva promoción. Solo un ADMIN puede hacerlo.
     *
     * Corrección de bug: antes no verificaba nada — cualquiera podía
     * crear una promoción (ej. 90% de descuento) que se suma
     * automáticamente al descuento de TODAS las reservas del sistema
     * mientras esté vigente (ver DiscountService).
     *
     * Regla: la fecha de término debe ser posterior a la de inicio.
     * Toda promoción nace activa (active = true).
     */
    public Promotion createPromotion(Long adminUserId, CreatePromotionRequest request) {
        accessControlService.requireAdmin(adminUserId);

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BusinessRuleException("End date must be after start date");
        }

        Promotion promotion = Promotion.builder()
                .name(request.getName())
                .description(request.getDescription())
                .discountPercentage(request.getDiscountPercentage())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .active(true)
                .build();

        return promotionRepository.save(promotion);
    }

    /**
     * LISTAR todas las promociones (activas e inactivas, vigentes o
     * no). Solo un ADMIN puede hacerlo.
     *
     * Corrección de bug: antes cualquiera podía ver el historial
     * completo de promociones, incluidas las inactivas/vencidas —
     * información de estrategia comercial interna.
     */
    @Transactional(readOnly = true)
    public List<Promotion> getAllPromotions(Long adminUserId) {
        accessControlService.requireAdmin(adminUserId);
        return promotionRepository.findAll();
    }

    /**
     * ACTIVAR o DESACTIVAR una promoción manualmente, sin necesidad de
     * esperar a que venza endDate ni de borrarla. Solo un ADMIN puede hacerlo.
     */
    public Promotion changeStatus(Long adminUserId, Long id, boolean active) {
        accessControlService.requireAdmin(adminUserId);

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id: " + id));
        promotion.setActive(active);
        return promotionRepository.save(promotion);
    }
}
