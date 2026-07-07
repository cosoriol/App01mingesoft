package com.travelagency.service;

import com.travelagency.dto.request.LoginRequest;
import com.travelagency.dto.request.UpdateProfileRequest;
import com.travelagency.dto.request.UserRegistrationRequest;
import com.travelagency.entity.User;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * SERVICIO DE USUARIOS (Capa Service)
 * =====================================
 * Registro e inicio de sesión básicos (email + contraseña propia).
 *
 * Épica 1: Gestión de usuarios y clientes
 *
 * NOTA: Esta autenticación es simple y local. Más adelante se
 * reemplazará por Keycloak (por eso User.keycloakId ya existe).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;

    /**
     * REGISTRAR un nuevo usuario.
     * La contraseña se guarda hasheada (BCrypt), nunca en texto plano.
     */
    public User register(UserRegistrationRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .identityDocument(request.getIdentityDocument())
                .nationality(request.getNationality())
                .role("CLIENT")
                .active(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * INICIAR SESIÓN.
     * Verifica email + contraseña. Mensaje de error genérico
     * (no distingue "email no existe" de "contraseña incorrecta")
     * para no dar pistas a quien intenta adivinar credenciales.
     */
    @Transactional(readOnly = true)
    public User login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Invalid email or password"));

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessRuleException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessRuleException("This account is deactivated");
        }

        return user;
    }

    /**
     * LISTAR todos los usuarios. Solo un ADMIN puede hacerlo.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers(Long adminUserId) {
        accessControlService.requireAdmin(adminUserId);
        return userRepository.findAll();
    }

    /**
     * OBTENER un usuario por ID (uso interno, sin control de acceso).
     * La usan changeActiveStatus/updateProfile, que hacen su propia
     * verificación antes de tocar al usuario objetivo.
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * OBTENER un usuario por ID, para un endpoint público.
     *
     * Corrección de bug: antes cualquiera podía ver el perfil completo
     * (incluido el documento de identidad) de cualquier usuario con
     * solo cambiar el id en la URL.
     */
    @Transactional(readOnly = true)
    public User getUserByIdForRequester(Long id, Long requestingUserId) {
        accessControlService.requireOwnerOrAdmin(requestingUserId, id);
        return getUserById(id);
    }

    /**
     * ACTIVAR o DESACTIVAR una cuenta. Solo un ADMIN puede hacerlo.
     *
     * Regla del proyecto (Épica 1): los usuarios NO se eliminan de la
     * base de datos, se desactivan (borrado lógico). Un usuario
     * desactivado no puede iniciar sesión (ver login()).
     */
    public User changeActiveStatus(Long adminUserId, Long targetUserId, boolean active) {
        accessControlService.requireAdmin(adminUserId);

        User targetUser = getUserById(targetUserId);
        targetUser.setActive(active);
        return userRepository.save(targetUser);
    }

    /**
     * ACTUALIZAR PERFIL de un usuario (nombre, teléfono, documento,
     * nacionalidad). Solo el propio dueño del perfil o un ADMIN
     * pueden editarlo — mismo patrón de "dueño o admin" que
     * BookingService.cancelBooking (Épica 6).
     *
     * Deliberadamente no toca email, password ni role (ver
     * UpdateProfileRequest para el porqué).
     */
    public User updateProfile(Long requestingUserId, Long targetUserId, UpdateProfileRequest request) {
        accessControlService.requireOwnerOrAdmin(requestingUserId, targetUserId);

        User targetUser = getUserById(targetUserId);
        targetUser.setFullName(request.getFullName());
        targetUser.setPhone(request.getPhone());
        targetUser.setIdentityDocument(request.getIdentityDocument());
        targetUser.setNationality(request.getNationality());
        return userRepository.save(targetUser);
    }
}
