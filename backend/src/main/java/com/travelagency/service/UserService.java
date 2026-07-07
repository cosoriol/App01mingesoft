package com.travelagency.service;

import com.travelagency.dto.request.LoginRequest;
import com.travelagency.dto.request.UserRegistrationRequest;
import com.travelagency.entity.User;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
