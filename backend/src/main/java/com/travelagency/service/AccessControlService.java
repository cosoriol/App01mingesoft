package com.travelagency.service;

import com.travelagency.entity.User;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * SERVICIO DE CONTROL DE ACCESO (Capa Service)
 * ================================================
 * Centraliza las dos reglas de autorización que se repiten en todo
 * el proyecto:
 *   1. "Solo un ADMIN puede hacer esto" (requireAdmin)
 *   2. "Solo el dueño del recurso, o un ADMIN, puede hacer esto"
 *      (requireOwnerOrAdmin)
 *
 * Como el proyecto todavía no tiene autenticación real vía tokens
 * (pendiente de Keycloak, ver SecurityConfig), cada endpoint recibe
 * el id de quien hace la petición como parámetro (?userId=X) y este
 * servicio verifica su rol/identidad contra la base de datos.
 *
 * Antes de existir esta clase, la misma verificación de 5 líneas
 * estaba copiada en BookingService, TravelPackageService y
 * UserService — y PromotionService y ReportService directamente no
 * la tenían (bug de seguridad real, corregido acá).
 */
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;

    /**
     * Lanza una excepción si el usuario no existe o no es ADMIN.
     * @return el usuario, ya verificado, por si el llamador lo necesita
     */
    public User requireAdmin(Long userId) {
        User user = findUser(userId);
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessRuleException("Only an ADMIN can perform this action");
        }
        return user;
    }

    /**
     * Lanza una excepción si quien pregunta no es el dueño del
     * recurso (mismo userId) ni un ADMIN.
     */
    public void requireOwnerOrAdmin(Long requestingUserId, Long resourceOwnerId) {
        User requestingUser = findUser(requestingUserId);
        boolean isOwner = requestingUserId.equals(resourceOwnerId);
        boolean isAdmin = "ADMIN".equals(requestingUser.getRole());
        if (!isOwner && !isAdmin) {
            throw new BusinessRuleException("You are not allowed to access this resource");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
