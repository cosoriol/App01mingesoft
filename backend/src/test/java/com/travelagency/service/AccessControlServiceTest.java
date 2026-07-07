package com.travelagency.service;

import com.travelagency.entity.User;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * TESTS UNITARIOS DE AccessControlService
 * ==========================================================
 * Este servicio centraliza el control de acceso de todo el proyecto
 * (antes duplicado en varios servicios, y ausente en otros — lo que
 * causó varios bugs de seguridad reales, corregidos en esta revisión).
 * Aquí se prueba la lógica en sí; los demás *ServiceTest solo
 * verifican que cada servicio la use correctamente.
 */
@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccessControlService accessControlService;

    private User buildUser(Long id, String role) {
        return User.builder().id(id).fullName("Usuario " + id).email("u" + id + "@example.com")
                .role(role).active(true).build();
    }

    // ============================================================
    // requireAdmin
    // ============================================================

    @Test
    void requireAdmin_positivo_usuarioConRolAdmin_noLanzaExcepcion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "ADMIN")));

        assertDoesNotThrow(() -> accessControlService.requireAdmin(1L));
    }

    @Test
    void requireAdmin_negativo_usuarioConRolClient_lanzaBusinessRuleException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, "CLIENT")));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> accessControlService.requireAdmin(2L));
        assertTrue(ex.getMessage().contains("ADMIN"));
    }

    @Test
    void requireAdmin_negativo_usuarioInexistente_lanzaResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accessControlService.requireAdmin(99L));
    }

    // ============================================================
    // requireOwnerOrAdmin
    // ============================================================

    @Test
    void requireOwnerOrAdmin_positivo_elMismoUsuario_noLanzaExcepcion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "CLIENT")));

        assertDoesNotThrow(() -> accessControlService.requireOwnerOrAdmin(1L, 1L));
    }

    @Test
    void requireOwnerOrAdmin_positivo_unAdminPuedeAccederARecursoAjeno() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, "ADMIN")));

        assertDoesNotThrow(() -> accessControlService.requireOwnerOrAdmin(2L, 1L));
    }

    @Test
    void requireOwnerOrAdmin_negativo_otroClienteNoPuedeAccederARecursoAjeno() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, "CLIENT")));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> accessControlService.requireOwnerOrAdmin(2L, 1L));
        assertTrue(ex.getMessage().contains("not allowed"));
    }
}
