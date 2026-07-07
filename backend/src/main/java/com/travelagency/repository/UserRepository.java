package com.travelagency.repository;

import com.travelagency.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * REPOSITORIO DE USUARIOS (Capa Repository)
 * ==========================================
 * Esta interfaz se conecta con la tabla "users" en la BD.
 *
 * JpaRepository ya incluye métodos automáticos:
 *   - save(user)      → guardar un usuario
 *   - findById(id)    → buscar por ID
 *   - findAll()       → obtener todos los usuarios
 *   - deleteById(id)  → eliminar por ID
 *   - count()         → contar registros
 *
 * Los métodos que escribimos aquí son consultas PERSONALIZADAS.
 * Spring Data JPA genera el SQL automáticamente a partir del
 * nombre del método. Ejemplo:
 *   findByEmail → SELECT * FROM users WHERE email = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su email.
     * Optional significa que puede devolver un usuario o estar vacío.
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario por su ID de Keycloak.
     */
    Optional<User> findByKeycloakId(String keycloakId);

    /**
     * Verifica si ya existe un usuario con ese email.
     * Retorna true o false.
     */
    boolean existsByEmail(String email);
}
