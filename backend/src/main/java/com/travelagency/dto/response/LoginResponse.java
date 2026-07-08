package com.travelagency.dto.response;

import com.travelagency.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO: respuesta de un login exitoso.
 * ====================================================================
 * "token" es por ahora un UUID simple sin significado criptográfico
 * (no es un JWT real); cuando se integre Keycloak, este campo pasará
 * a ser el access token que emita Keycloak.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private User user;
    private String token;
    private String message;
}
