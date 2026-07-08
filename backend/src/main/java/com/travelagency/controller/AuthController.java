package com.travelagency.controller;

import com.travelagency.dto.request.LoginRequest;
import com.travelagency.dto.request.UserRegistrationRequest;
import com.travelagency.dto.response.LoginResponse;
import com.travelagency.entity.User;
import com.travelagency.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CONTROLADOR DE AUTENTICACIÓN (Capa Controller)
 * ================================================
 * Endpoints de registro e inicio de sesión.
 *
 * Épica 1: Gestión de usuarios y clientes
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    /**
     * REGISTRAR un nuevo usuario (cliente)
     * URL: POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody UserRegistrationRequest request) {
        User created = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * INICIAR SESIÓN
     * URL: POST /api/auth/login
     *
     * "token" es por ahora un UUID simple (sin significado
     * criptográfico); cuando se integre Keycloak, este campo pasará a
     * ser el access token real emitido por Keycloak.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request);
        String token = UUID.randomUUID().toString();
        return ResponseEntity.ok(new LoginResponse(user, token, "Login exitoso"));
    }
}
