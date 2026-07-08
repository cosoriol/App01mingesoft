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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TESTS UNITARIOS DE UserService (Épica 1: Gestión de usuarios y clientes)
 * ============================================================================
 * Cubre registro, login, y las funciones de administración agregadas
 * (listar usuarios, activar/desactivar cuentas). Se mockean el
 * repositorio y el PasswordEncoder.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    /**
     * UserService depende de AccessControlService (que a su vez usa
     * UserRepository). Se construye una instancia REAL apoyada en el
     * mismo userRepository ya mockeado, en vez de mockear
     * AccessControlService. SecurityLockoutConfig se instancia
     * directamente (es un simple POJO de configuración, no un bean
     * que valga la pena mockear) con los valores por defecto
     * (5 intentos, 30 minutos de bloqueo).
     */
    @BeforeEach
    void setUp() {
        AccessControlService accessControlService = new AccessControlService(userRepository);
        userService = new UserService(
                userRepository, passwordEncoder, accessControlService, new SecurityLockoutConfig());
    }

    private User buildUser(Long id, String role, boolean active) {
        return User.builder().id(id).fullName("Usuario " + id).email("u" + id + "@example.com")
                .password("hashed-password").role(role).active(active).failedLoginAttempts(0).build();
    }

    /** Contraseña que SÍ cumple las reglas de fuerza (mayúscula, minúscula, número, especial, 8+ chars). */
    private static final String STRONG_PASSWORD = "Password123!";

    private UserRegistrationRequest buildRegistrationRequest() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setFullName("Ana Torres");
        request.setEmail("ana.torres@example.com");
        request.setPassword(STRONG_PASSWORD);
        return request;
    }

    // ============================================================
    // REGISTRO
    // ============================================================

    @Test
    void registro_positivo_emailNuevo_creaElUsuarioComoClient() {
        when(userRepository.existsByEmail("ana.torres@example.com")).thenReturn(false);
        when(passwordEncoder.encode(STRONG_PASSWORD)).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.register(buildRegistrationRequest());

        assertEquals("CLIENT", result.getRole());
        assertEquals("hashed-password", result.getPassword());
        assertTrue(result.getActive());
    }

    @Test
    void registro_negativo_emailYaRegistrado_lanzaBusinessRuleException() {
        when(userRepository.existsByEmail("ana.torres@example.com")).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> userService.register(buildRegistrationRequest()));
        verify(userRepository, never()).save(any());
    }

    // ============================================================
    // FUERZA DE LA CONTRASEÑA (Regla 3)
    // ============================================================

    @Test
    void registro_negativo_contraseñaSinMayuscula_lanzaBusinessRuleException() {
        when(userRepository.existsByEmail("ana.torres@example.com")).thenReturn(false);
        UserRegistrationRequest request = buildRegistrationRequest();
        request.setPassword("password123!");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.register(request));
        assertTrue(ex.getMessage().contains("uppercase"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registro_negativo_contraseñaSinMinuscula_lanzaBusinessRuleException() {
        when(userRepository.existsByEmail("ana.torres@example.com")).thenReturn(false);
        UserRegistrationRequest request = buildRegistrationRequest();
        request.setPassword("PASSWORD123!");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.register(request));
        assertTrue(ex.getMessage().contains("lowercase"));
    }

    @Test
    void registro_negativo_contraseñaSinNumero_lanzaBusinessRuleException() {
        when(userRepository.existsByEmail("ana.torres@example.com")).thenReturn(false);
        UserRegistrationRequest request = buildRegistrationRequest();
        request.setPassword("Password!");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.register(request));
        assertTrue(ex.getMessage().contains("number"));
    }

    @Test
    void registro_negativo_contraseñaSinCaracterEspecial_lanzaBusinessRuleException() {
        when(userRepository.existsByEmail("ana.torres@example.com")).thenReturn(false);
        UserRegistrationRequest request = buildRegistrationRequest();
        request.setPassword("Password123");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.register(request));
        assertTrue(ex.getMessage().contains("special character"));
    }

    @Test
    void registro_negativo_contraseñaMenosDe8Caracteres_lanzaBusinessRuleException() {
        when(userRepository.existsByEmail("ana.torres@example.com")).thenReturn(false);
        UserRegistrationRequest request = buildRegistrationRequest();
        request.setPassword("Pw1!");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.register(request));
        assertTrue(ex.getMessage().contains("8 characters"));
    }

    // ============================================================
    // LOGIN
    // ============================================================

    @Test
    void login_positivo_credencialesCorrectas_devuelveElUsuario() {
        User user = buildUser(1L, "CLIENT", true);
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("u1@example.com");
        request.setPassword("password123");

        assertEquals(user, userService.login(request));
    }

    @Test
    void login_negativo_contraseñaIncorrecta_lanzaBusinessRuleException() {
        User user = buildUser(1L, "CLIENT", true);
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("u1@example.com");
        request.setPassword("wrong");

        assertThrows(BusinessRuleException.class, () -> userService.login(request));
    }

    @Test
    void login_negativo_emailInexistente_lanzaBusinessRuleException() {
        when(userRepository.findByEmail("nadie@example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("nadie@example.com");
        request.setPassword("password123");

        assertThrows(BusinessRuleException.class, () -> userService.login(request));
    }

    @Test
    void login_negativo_cuentaDesactivada_lanzaBusinessRuleException() {
        User inactiveUser = buildUser(1L, "CLIENT", false);
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("u1@example.com");
        request.setPassword("password123");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.login(request));
        assertTrue(ex.getMessage().contains("deactivated"));
    }

    // ============================================================
    // BLOQUEO POR INTENTOS FALLIDOS (Regla 6)
    // ============================================================

    @Test
    void login_negativo_contraseñaIncorrecta_incrementaIntentosFallidos() {
        User user = buildUser(1L, "CLIENT", true);
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("u1@example.com");
        request.setPassword("wrong");

        assertThrows(BusinessRuleException.class, () -> userService.login(request));

        assertEquals(1, user.getFailedLoginAttempts());
        assertNull(user.getLockUntil()); // todavía no llega al máximo (5)
    }

    @Test
    void login_negativo_bloqueadaDespuesDe5IntentosFallidos_lanzaBusinessRuleExceptionYQuedaBloqueada() {
        User user = buildUser(1L, "CLIENT", true);
        user.setFailedLoginAttempts(4); // este intento fallido será el 5to
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("u1@example.com");
        request.setPassword("wrong");

        assertThrows(BusinessRuleException.class, () -> userService.login(request));

        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockUntil());
        assertTrue(user.getLockUntil().isAfter(LocalDateTime.now()));
    }

    @Test
    void login_negativo_cuentaBloqueada_lanzaBusinessRuleExceptionSinValidarContraseña() {
        User user = buildUser(1L, "CLIENT", true);
        user.setFailedLoginAttempts(5);
        user.setLockUntil(LocalDateTime.now().plusMinutes(10)); // aún bloqueada
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("u1@example.com");
        request.setPassword("password123"); // aunque sea la correcta, la cuenta sigue bloqueada

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.login(request));
        assertTrue(ex.getMessage().contains("locked"));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_positivo_loginExitoso_reseteaIntentosFallidosYBloqueo() {
        User user = buildUser(1L, "CLIENT", true);
        user.setFailedLoginAttempts(3);
        user.setLockUntil(null);
        when(userRepository.findByEmail("u1@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("u1@example.com");
        request.setPassword("password123");

        userService.login(request);

        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockUntil());
    }

    // ============================================================
    // LISTAR USUARIOS (solo ADMIN)
    // ============================================================

    @Test
    void listarUsuarios_positivo_adminPuedeListarATodos() {
        User admin = buildUser(1L, "ADMIN", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin, buildUser(2L, "CLIENT", true)));

        List<User> result = userService.getAllUsers(1L);

        assertEquals(2, result.size());
    }

    @Test
    void listarUsuarios_negativo_clienteNoPuedeListarUsuarios() {
        User client = buildUser(2L, "CLIENT", true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class, () -> userService.getAllUsers(2L));
        verify(userRepository, never()).findAll();
    }

    // ============================================================
    // VER PERFIL DE UN USUARIO (dueño o ADMIN)
    // ============================================================

    @Test
    void verPerfil_positivo_elPropioUsuarioVeSuPerfil() {
        User user = buildUser(1L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.getUserByIdForRequester(1L, 1L));
    }

    @Test
    void verPerfil_positivo_adminVeElPerfilDeOtroUsuario() {
        User admin = buildUser(2L, "ADMIN", true);
        User target = buildUser(1L, "CLIENT", true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        assertDoesNotThrow(() -> userService.getUserByIdForRequester(1L, 2L));
    }

    @Test
    void verPerfil_negativo_otroClienteNoPuedeVerPerfilAjeno() {
        // Corrección de bug: antes cualquiera veía el perfil completo
        // (incluido el documento de identidad) de cualquier usuario.
        User intruso = buildUser(2L, "CLIENT", true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        assertThrows(BusinessRuleException.class,
                () -> userService.getUserByIdForRequester(1L, 2L));
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR CUENTA (solo ADMIN)
    // ============================================================

    @Test
    void cambiarEstado_positivo_adminDesactivaUnaCuenta() {
        User admin = buildUser(1L, "ADMIN", true);
        User target = buildUser(2L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.changeActiveStatus(1L, 2L, false);

        assertFalse(result.getActive());
    }

    @Test
    void cambiarEstado_negativo_clienteNoPuedeDesactivarCuentas() {
        User client = buildUser(3L, "CLIENT", true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class,
                () -> userService.changeActiveStatus(3L, 2L, false));
        verify(userRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_negativo_usuarioObjetivoInexistente_lanzaResourceNotFoundException() {
        User admin = buildUser(1L, "ADMIN", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.changeActiveStatus(1L, 99L, false));
    }

    // ============================================================
    // ACTUALIZAR PERFIL (dueño o ADMIN)
    // ============================================================

    private UpdateProfileRequest buildProfileRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Ana Torres Actualizada");
        request.setPhone("+56999999999");
        request.setIdentityDocument("11111111-1");
        request.setNationality("Chilena");
        return request;
    }

    @Test
    void actualizarPerfil_positivo_elPropioUsuarioEditaSuPerfil() {
        User self = buildUser(1L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateProfile(1L, 1L, buildProfileRequest());

        assertEquals("Ana Torres Actualizada", result.getFullName());
        assertEquals("+56999999999", result.getPhone());
    }

    @Test
    void actualizarPerfil_positivo_adminEditaElPerfilDeOtroUsuario() {
        User admin = buildUser(1L, "ADMIN", true);
        User target = buildUser(2L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateProfile(1L, 2L, buildProfileRequest());

        assertEquals("Ana Torres Actualizada", result.getFullName());
    }

    @Test
    void actualizarPerfil_negativo_clienteNoPuedeEditarElPerfilDeOtro() {
        User client = buildUser(3L, "CLIENT", true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(client));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.updateProfile(3L, 2L, buildProfileRequest()));

        assertTrue(ex.getMessage().contains("not allowed"));
        verify(userRepository, never()).save(any());
    }

    // ============================================================
    // CAMBIAR CONTRASEÑA (dueño o ADMIN, requiere la actual)
    // ============================================================

    private ChangePasswordRequest buildChangePasswordRequest(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    @Test
    void cambiarContraseña_positivo_elPropioUsuarioCambiaSuContraseña() {
        User user = buildUser(1L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123!", "hashed-password")).thenReturn(true);
        when(passwordEncoder.encode(STRONG_PASSWORD)).thenReturn("new-hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.changePassword(1L, 1L, buildChangePasswordRequest("OldPass123!", STRONG_PASSWORD));

        assertEquals("new-hashed-password", result.getPassword());
    }

    @Test
    void cambiarContraseña_negativo_contraseñaActualIncorrecta_lanzaBusinessRuleException() {
        User user = buildUser(1L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-current", "hashed-password")).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> userService.changePassword(1L, 1L, buildChangePasswordRequest("wrong-current", STRONG_PASSWORD)));

        assertTrue(ex.getMessage().contains("Current password is incorrect"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void cambiarContraseña_negativo_nuevaContraseñaDebil_lanzaBusinessRuleException() {
        User user = buildUser(1L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123!", "hashed-password")).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> userService.changePassword(1L, 1L, buildChangePasswordRequest("OldPass123!", "weak")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void cambiarContraseña_negativo_otroClienteNoPuedeCambiarContraseñaAjena() {
        User intruso = buildUser(2L, "CLIENT", true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        assertThrows(BusinessRuleException.class,
                () -> userService.changePassword(2L, 1L, buildChangePasswordRequest("x", STRONG_PASSWORD)));
    }

    // ============================================================
    // DESACTIVAR / REACTIVAR CUENTA (self-service + ADMIN)
    // ============================================================

    @Test
    void desactivarCuenta_positivo_elPropioUsuarioDesactivaSuCuenta() {
        User user = buildUser(1L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.deactivateAccount(1L, 1L);

        assertFalse(result.getActive());
    }

    @Test
    void desactivarCuenta_positivo_adminDesactivaLaCuentaDeOtroUsuario() {
        User admin = buildUser(1L, "ADMIN", true);
        User target = buildUser(2L, "CLIENT", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.deactivateAccount(1L, 2L);

        assertFalse(result.getActive());
    }

    @Test
    void desactivarCuenta_negativo_otroClienteNoPuedeDesactivarCuentaAjena() {
        User intruso = buildUser(2L, "CLIENT", true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(intruso));

        assertThrows(BusinessRuleException.class, () -> userService.deactivateAccount(2L, 1L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void reactivarCuenta_positivo_adminReactivaUnaCuenta() {
        User admin = buildUser(1L, "ADMIN", true);
        User target = buildUser(2L, "CLIENT", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.reactivateAccount(1L, 2L);

        assertTrue(result.getActive());
    }

    @Test
    void reactivarCuenta_negativo_clienteNoPuedeReactivarCuentas() {
        User client = buildUser(3L, "CLIENT", false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class, () -> userService.reactivateAccount(3L, 2L));
        verify(userRepository, never()).save(any());
    }
}
