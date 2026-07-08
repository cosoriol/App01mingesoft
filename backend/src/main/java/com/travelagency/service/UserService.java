package com.travelagency.service;

import com.travelagency.config.SecurityLockoutConfig;
import com.travelagency.dto.request.ChangePasswordRequest;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

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

    /** Regla 3: la contraseña debe tener al menos una mayúscula. */
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    /** Regla 3: la contraseña debe tener al menos una minúscula. */
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    /** Regla 3: la contraseña debe tener al menos un número. */
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    /** Regla 3: la contraseña debe tener al menos un carácter especial. */
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[@#$%^&+=!*]");
    /** Regla 4: formato de email (además de @Email en el DTO). */
    private static final Pattern EMAIL_FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;
    private final SecurityLockoutConfig securityLockoutConfig;

    /**
     * REGISTRAR un nuevo usuario.
     * La contraseña se guarda hasheada (BCrypt), nunca en texto plano.
     */
    public User register(UserRegistrationRequest request) {
        // Regla 1: el email debe ser único.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email already registered: " + request.getEmail());
        }

        // Regla 4: formato de email (revalidado en el service, además
        // del @Email del DTO, por si register() se invoca directamente).
        if (!EMAIL_FORMAT.matcher(request.getEmail()).matches()) {
            throw new BusinessRuleException("Invalid email format");
        }

        // Regla 3: la contraseña debe cumplir los requisitos de seguridad.
        validatePasswordStrength(request.getPassword());

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
     * REGLA 3: valida que la contraseña cumpla los requisitos de
     * seguridad mínimos (largo, mayúscula, minúscula, número, carácter
     * especial), indicando explícitamente cuál requisito falta.
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessRuleException("Password must be at least 8 characters long");
        }
        if (!UPPERCASE.matcher(password).find()) {
            throw new BusinessRuleException("Password must contain at least one uppercase letter");
        }
        if (!LOWERCASE.matcher(password).find()) {
            throw new BusinessRuleException("Password must contain at least one lowercase letter");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new BusinessRuleException("Password must contain at least one number");
        }
        if (!SPECIAL_CHAR.matcher(password).find()) {
            throw new BusinessRuleException("Password must contain at least one special character (@#$%^&+=!*)");
        }
    }

    /**
     * INICIAR SESIÓN.
     * Verifica bloqueo por intentos fallidos (Regla 6), luego
     * contraseña, luego que la cuenta esté activa (Regla 5). El
     * mensaje de "credenciales inválidas" es genérico (no distingue
     * "email no existe" de "contraseña incorrecta") para no dar
     * pistas a quien intenta adivinar credenciales; el bloqueo y la
     * desactivación sí llevan un mensaje específico porque son
     * estados de negocio, no pistas sobre la contraseña.
     *
     * noRollbackFor: por defecto, @Transactional deshace TODO cuando
     * el método termina lanzando una RuntimeException (como
     * BusinessRuleException) — lo que borraría silenciosamente el
     * incremento de failedLoginAttempts que handleFailedLogin() ya
     * guardó, dejando el bloqueo de cuenta completamente inoperante.
     */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public User login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Invalid email or password"));

        // Regla 6: la cuenta puede estar temporalmente bloqueada por
        // exceso de intentos fallidos.
        if (isAccountLocked(user)) {
            throw new BusinessRuleException(
                    "Account is temporarily locked due to too many failed login attempts. "
                    + "Please try again later.");
        }

        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new BusinessRuleException("Invalid email or password");
        }

        // Regla 5: cuenta desactivada no puede iniciar sesión (con
        // contraseña correcta, así que no hay ambigüedad de credenciales
        // que proteger aquí).
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessRuleException("Account is deactivated. Please contact support.");
        }

        // Login exitoso: resetea el contador de intentos fallidos y el bloqueo.
        user.setFailedLoginAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);

        return user;
    }

    /** Regla 6: ¿la cuenta está bloqueada ahora mismo? */
    private boolean isAccountLocked(User user) {
        return user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Regla 6: registra un intento fallido; si alcanza el máximo
     * configurado, bloquea la cuenta por el tiempo configurado.
     */
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= securityLockoutConfig.getMaxFailedAttempts()) {
            user.setLockUntil(LocalDateTime.now().plusMinutes(securityLockoutConfig.getLockDurationMinutes()));
        }
        userRepository.save(user);
    }

    /**
     * CAMBIAR LA CONTRASEÑA de un usuario. El dueño de la cuenta, o
     * un ADMIN, pueden hacerlo — igual que updateProfile. En la
     * práctica solo el propio dueño puede tener éxito, ya que se
     * exige conocer la contraseña ACTUAL para cambiarla.
     */
    public User changePassword(Long requestingUserId, Long targetUserId, ChangePasswordRequest request) {
        accessControlService.requireOwnerOrAdmin(requestingUserId, targetUserId);

        User user = getUserById(targetUserId);
        if (user.getPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessRuleException("Current password is incorrect");
        }

        validatePasswordStrength(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        return userRepository.save(user);
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
     * DESACTIVAR una cuenta (borrado lógico). A diferencia de
     * changeActiveStatus (solo ADMIN, pensado para administración
     * masiva), este método también permite que un usuario desactive
     * SU PROPIA cuenta (ej. "eliminar mi cuenta" desde su perfil).
     */
    public User deactivateAccount(Long requestingUserId, Long targetUserId) {
        accessControlService.requireOwnerOrAdmin(requestingUserId, targetUserId);

        User targetUser = getUserById(targetUserId);
        targetUser.setActive(false);
        return userRepository.save(targetUser);
    }

    /**
     * REACTIVAR una cuenta previamente desactivada. Solo un ADMIN
     * puede hacerlo (un usuario no puede reactivarse a sí mismo,
     * ya que si su cuenta está desactivada no puede loguearse para
     * pedirlo).
     */
    public User reactivateAccount(Long adminUserId, Long targetUserId) {
        accessControlService.requireAdmin(adminUserId);

        User targetUser = getUserById(targetUserId);
        targetUser.setActive(true);
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
