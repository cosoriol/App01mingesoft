package com.travelagency.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ENTIDAD USER (Tabla "users" en la base de datos)
 * ================================================
 * Representa a un usuario del sistema (cliente o administrador).
 *
 * Épica 1: Gestión de usuarios y clientes
 *
 * @Entity    = le dice a JPA que esta clase es una tabla en la BD
 * @Table     = define el nombre de la tabla
 * @Data      = Lombok genera automáticamente getters, setters, toString, etc.
 * @Builder   = permite crear objetos con el patrón Builder
 * @NoArgsConstructor = genera constructor vacío (requerido por JPA)
 * @AllArgsConstructor = genera constructor con todos los campos
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * ID único del usuario.
     * @Id = indica que es la clave primaria
     * @GeneratedValue = el ID se genera automáticamente (1, 2, 3...)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo del usuario.
     * nullable = false: es obligatorio (no puede estar vacío)
     */
    @Column(nullable = false)
    private String fullName;

    /**
     * Correo electrónico del usuario.
     * unique = true: no puede haber dos usuarios con el mismo email
     * nullable = false: es obligatorio
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Contraseña del usuario (hasheada con BCrypt).
     * @JsonIgnore evita que se devuelva en las respuestas JSON de la API.
     */
    @JsonIgnore
    private String password;

    /**
     * Teléfono del usuario (opcional).
     */
    private String phone;

    /**
     * Documento de identidad (opcional, útil para viajes).
     */
    private String identityDocument;

    /**
     * Nacionalidad del usuario (opcional).
     */
    private String nationality;

    /**
     * ID del usuario en Keycloak.
     * Conecta el usuario local con el usuario en el sistema de autenticación.
     */
    @Column(unique = true)
    private String keycloakId;

    /**
     * Rol del usuario: "CLIENT" o "ADMIN".
     * Determina qué funcionalidades puede usar.
     */
    @Column(nullable = false)
    private String role;

    /**
     * ¿Está activa la cuenta?
     * true = activa, false = desactivada (borrado lógico)
     * Regla: no se eliminan usuarios, se desactivan
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Intentos de login fallidos consecutivos. Se resetea a 0 en
     * cada login exitoso. Al alcanzar el máximo configurado
     * (app.security.max-failed-attempts), la cuenta se bloquea
     * temporalmente (ver lockUntil).
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * Fecha/hora hasta la cual la cuenta queda bloqueada por exceso
     * de intentos fallidos. Null = no está bloqueada. nullable a
     * nivel de columna a propósito: la mayoría de las cuentas nunca
     * llegan a bloquearse.
     */
    private LocalDateTime lockUntil;

    /**
     * Fecha y hora en que se creó la cuenta.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Fecha de la última actualización del perfil.
     */
    private LocalDateTime updatedAt;

    /**
     * Método que se ejecuta automáticamente ANTES de guardar
     * un nuevo usuario en la BD. Establece las fechas.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Se ejecuta automáticamente ANTES de actualizar un usuario.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
