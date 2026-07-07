package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para REGISTRAR un nuevo usuario.
 */
@Data
public class UserRegistrationRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String phone;
    private String identityDocument;
    private String nationality;
}
