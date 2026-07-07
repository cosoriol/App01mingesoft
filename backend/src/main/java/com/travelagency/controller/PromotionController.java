package com.travelagency.controller;

import com.travelagency.dto.request.CreatePromotionRequest;
import com.travelagency.entity.Promotion;
import com.travelagency.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLADOR DE PROMOCIONES (Capa Controller)
 * ===============================================
 * Épica 4 (Regla 5): promociones por tiempo limitado.
 *
 * Endpoints de administración de promociones. En el flujo de reserva,
 * las promociones vigentes se aplican automáticamente (ver
 * DiscountService); estos endpoints son para crearlas y gestionarlas.
 */
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromotionController {

    private final PromotionService promotionService;

    /**
     * CREAR una nueva promoción.
     * URL: POST /api/promotions?userId=3
     * Acceso: solo ADMIN
     */
    @PostMapping
    public ResponseEntity<Promotion> createPromotion(
            @RequestParam Long userId,
            @Valid @RequestBody CreatePromotionRequest request) {
        Promotion created = promotionService.createPromotion(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * LISTAR todas las promociones.
     * URL: GET /api/promotions?userId=3
     * Acceso: solo ADMIN
     */
    @GetMapping
    public ResponseEntity<List<Promotion>> getAllPromotions(@RequestParam Long userId) {
        return ResponseEntity.ok(promotionService.getAllPromotions(userId));
    }

    /**
     * ACTIVAR/DESACTIVAR una promoción.
     * URL: PATCH /api/promotions/5/status?active=false&userId=3
     * Acceso: solo ADMIN
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Promotion> changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            @RequestParam Long userId) {
        return ResponseEntity.ok(promotionService.changeStatus(userId, id, active));
    }
}
