package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para INICIAR SESIÓN.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
