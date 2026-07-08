package com.travelagency.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para CAMBIAR la contraseña de un usuario.
 * newPassword se valida con las mismas reglas de fuerza que el
 * registro (ver UserService.validatePasswordStrength).
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
