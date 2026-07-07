package com.travelagency.controller;

import com.travelagency.dto.request.UpdateProfileRequest;
import com.travelagency.entity.User;
import com.travelagency.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * CONTROLADOR DE USUARIOS (Capa Controller)
 * ============================================
 * Épica 1: Gestión de usuarios y clientes.
 *
 * Endpoints de administración de cuentas (registro/login viven en
 * AuthController). Los usuarios no se eliminan, solo se activan o
 * desactivan (borrado lógico).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    /**
     * LISTAR todos los usuarios.
     * URL: GET /api/users?userId=3   (userId = admin que consulta)
     * Acceso: solo ADMIN
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(@RequestParam Long userId) {
        return ResponseEntity.ok(userService.getAllUsers(userId));
    }

    /**
     * OBTENER un usuario por ID.
     * URL: GET /api/users/5?userId=5
     * Acceso: el propio usuario, o un ADMIN
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(userService.getUserByIdForRequester(id, userId));
    }

    /**
     * ACTIVAR/DESACTIVAR una cuenta.
     * URL: PATCH /api/users/5/status?active=false&userId=3
     * Acceso: solo ADMIN
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<User> changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            @RequestParam Long userId) {
        return ResponseEntity.ok(userService.changeActiveStatus(userId, id, active));
    }

    /**
     * ACTUALIZAR el perfil de un usuario (nombre, teléfono, documento,
     * nacionalidad).
     * URL: PUT /api/users/5?userId=5   (editando el propio perfil)
     * Acceso: el propio dueño del perfil, o un ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateProfile(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, id, request));
    }
}
