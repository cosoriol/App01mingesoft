package com.travelagency.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para ACTUALIZAR el perfil de un usuario.
 * Épica 1: Gestión de usuarios y clientes.
 *
 * Deliberadamente NO incluye email, password ni role: cambiar el
 * email rompería el login (que busca por email), la contraseña
 * necesita su propio flujo (verificar la actual, etc.) y el rol solo
 * lo puede tocar un ADMIN por una vía distinta. Este DTO es solo para
 * los datos de contacto/perfil del propio usuario.
 */
@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;
    private String identityDocument;
    private String nationality;
}
