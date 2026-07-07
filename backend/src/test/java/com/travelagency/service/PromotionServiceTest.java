package com.travelagency.service;

import com.travelagency.dto.request.CreatePromotionRequest;
import com.travelagency.entity.Promotion;
import com.travelagency.entity.User;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.PromotionRepository;
import com.travelagency.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TESTS UNITARIOS DE PromotionService (Épica 4, Regla 5: promociones)
 * ======================================================================
 * Esta clase nunca había tenido tests. Se agregan ahora junto con la
 * corrección de un bug de seguridad: antes CUALQUIERA (sin loguearse)
 * podía crear o desactivar promociones, las cuales se suman
 * automáticamente al descuento de TODAS las reservas del sistema
 * mientras estén vigentes (ver DiscountService).
 */
@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private UserRepository userRepository;

    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        AccessControlService accessControlService = new AccessControlService(userRepository);
        promotionService = new PromotionService(promotionRepository, accessControlService);
    }

    private User buildUser(Long id, String role) {
        return User.builder().id(id).fullName("Usuario " + id).email("u" + id + "@example.com")
                .role(role).active(true).build();
    }

    private CreatePromotionRequest buildValidRequest() {
        CreatePromotionRequest request = new CreatePromotionRequest();
        request.setName("Verano 2026");
        request.setDescription("Promoción de temporada alta");
        request.setDiscountPercentage(new BigDecimal("7.5"));
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(30));
        return request;
    }

    // ============================================================
    // CREAR PROMOCIÓN (solo ADMIN) — corrección de bug
    // ============================================================

    @Test
    void crear_positivo_adminPuedeCrearUnaPromocion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "ADMIN")));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        Promotion result = promotionService.createPromotion(1L, buildValidRequest());

        assertEquals("Verano 2026", result.getName());
        assertTrue(result.getActive());
    }

    @Test
    void crear_negativo_clienteNoPuedeCrearUnaPromocion() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, "CLIENT")));

        assertThrows(BusinessRuleException.class,
                () -> promotionService.createPromotion(2L, buildValidRequest()));
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void crear_negativo_fechaFinNoEsPosteriorAFechaInicio_lanzaBusinessRuleException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "ADMIN")));

        CreatePromotionRequest request = buildValidRequest();
        request.setEndDate(request.getStartDate());

        assertThrows(BusinessRuleException.class,
                () -> promotionService.createPromotion(1L, request));
    }

    // ============================================================
    // ACTIVAR/DESACTIVAR PROMOCIÓN (solo ADMIN) — corrección de bug
    // ============================================================

    @Test
    void cambiarEstado_positivo_adminPuedeDesactivarUnaPromocion() {
        Promotion promotion = Promotion.builder()
                .id(1L).name("Verano 2026").discountPercentage(new BigDecimal("7.5"))
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(30))
                .active(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "ADMIN")));
        when(promotionRepository.findById(1L)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> inv.getArgument(0));

        Promotion result = promotionService.changeStatus(1L, 1L, false);

        assertFalse(result.getActive());
    }

    @Test
    void cambiarEstado_negativo_clienteNoPuedeDesactivarUnaPromocion() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, "CLIENT")));

        assertThrows(BusinessRuleException.class,
                () -> promotionService.changeStatus(2L, 1L, false));
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_negativo_promocionInexistente_lanzaResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "ADMIN")));
        when(promotionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> promotionService.changeStatus(1L, 99L, false));
    }

    // ============================================================
    // LISTAR TODAS LAS PROMOCIONES (solo ADMIN) — corrección de bug
    // ============================================================

    @Test
    void listar_positivo_adminPuedeVerElHistorialCompletoDePromociones() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "ADMIN")));
        when(promotionRepository.findAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> promotionService.getAllPromotions(1L));
        verify(promotionRepository).findAll();
    }

    @Test
    void listar_negativo_clienteNoPuedeVerElHistorialDePromociones() {
        // Corrección de bug: antes cualquiera veía el historial
        // completo (incluidas promociones inactivas/vencidas),
        // información de estrategia comercial interna.
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, "CLIENT")));

        assertThrows(BusinessRuleException.class, () -> promotionService.getAllPromotions(2L));
        verify(promotionRepository, never()).findAll();
    }
}
