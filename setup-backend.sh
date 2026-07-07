#!/bin/bash
# ============================================================
# SETUP BACKEND - Proyecto TravelAgency
# ============================================================
# Crea la estructura y el código del Backend (Spring Boot + Java).
#
# Puede ejecutarse solo, o ser llamado por setup-project.sh
#
# USO: Ejecutar desde ~/App01Mingesoft/
#   chmod +x setup-backend.sh
#   ./setup-backend.sh
# ============================================================

# ============================================================
# 1. ESTRUCTURA DEL BACKEND (Spring Boot)
# ============================================================
echo "📦 Creando estructura del Backend..."

# Carpeta raíz del backend
mkdir -p backend

# Crear estructura Maven estándar
mkdir -p backend/src/main/java/com/travelagency/config
mkdir -p backend/src/main/java/com/travelagency/controller
mkdir -p backend/src/main/java/com/travelagency/dto/request
mkdir -p backend/src/main/java/com/travelagency/dto/response
mkdir -p backend/src/main/java/com/travelagency/entity
mkdir -p backend/src/main/java/com/travelagency/exception
mkdir -p backend/src/main/java/com/travelagency/repository
mkdir -p backend/src/main/java/com/travelagency/service
mkdir -p backend/src/main/resources
mkdir -p backend/src/test/java/com/travelagency/service

echo "   ✅ Estructura de carpetas del backend creada"

# ---- pom.xml (configuración Maven) ----
cat > backend/pom.xml << 'POMEOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot como proyecto padre -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <!-- Datos del proyecto -->
    <groupId>com.travelagency</groupId>
    <artifactId>travelagency-backend</artifactId>
    <version>1.0.0</version>
    <name>TravelAgency Backend</name>
    <description>Backend para la plataforma TravelAgency</description>

    <!-- Versión de Java -->
    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Web: para crear APIs REST (endpoints HTTP) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA: para conectar con la base de datos sin escribir SQL -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Spring Validation: para validar datos de entrada -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Security + OAuth2: para integrar Keycloak -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>

        <!-- MySQL Driver: para conectar con la base de datos MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok: reduce código repetitivo (getters, setters, constructores) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- ===== DEPENDENCIAS DE TESTING ===== -->

        <!-- Spring Boot Test: framework base para pruebas -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- H2 Database: base de datos en memoria para pruebas -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Spring Security Test: utilidades para probar seguridad -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Plugin de Spring Boot para empaquetar la aplicación -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Plugin JaCoCo: mide la cobertura de pruebas (necesitas 90%) -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.12</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
POMEOF

echo "   ✅ pom.xml creado"

# ---- application.properties ----
cat > backend/src/main/resources/application.properties << 'APPEOF'
# ============================================================
# CONFIGURACIÓN DE LA APLICACIÓN - TravelAgency
# ============================================================

# --- Nombre y puerto de la aplicación ---
spring.application.name=travelagency
server.port=8080

# --- Conexión a la Base de Datos MySQL ---
# "travelagency_db" es el nombre de la base de datos
# Cambia localhost por el host de Docker si usas contenedores
spring.datasource.url=jdbc:mysql://localhost:3306/travelagency_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=root123

# --- JPA / Hibernate ---
# "update" = Hibernate actualiza las tablas automáticamente al iniciar
# En producción se usa "validate" o "none"
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true

# --- Keycloak / OAuth2 ---
# Configura esto cuando tengas Keycloak corriendo
# spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/travelagency
# spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/travelagency/protocol/openid-connect/certs

# --- CORS (permitir peticiones del frontend) ---
app.cors.allowed-origins=http://localhost:3000
APPEOF

echo "   ✅ application.properties creado"

# ---- Clase principal de la aplicación ----
cat > backend/src/main/java/com/travelagency/TravelAgencyApplication.java << 'MAINEOF'
package com.travelagency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación TravelAgency.
 *
 * @SpringBootApplication es la anotación que le dice a Spring Boot
 * que esta es la clase de inicio. Combina tres anotaciones:
 *   - @Configuration: indica que es una clase de configuración
 *   - @EnableAutoConfiguration: activa la configuración automática
 *   - @ComponentScan: busca automáticamente todas las clases del proyecto
 */
@SpringBootApplication
public class TravelAgencyApplication {

    public static void main(String[] args) {
        // Este método inicia toda la aplicación Spring Boot
        SpringApplication.run(TravelAgencyApplication.class, args);
    }
}
MAINEOF

echo "   ✅ TravelAgencyApplication.java creado"

# ============================================================
# ENTIDADES (Tablas de la base de datos)
# ============================================================
echo "📦 Creando Entidades..."

# ---- Entidad User ----
cat > backend/src/main/java/com/travelagency/entity/User.java << 'USEREOF'
package com.travelagency.entity;

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
USEREOF

echo "   ✅ User.java creado"

# ---- Entidad TravelPackage ----
cat > backend/src/main/java/com/travelagency/entity/TravelPackage.java << 'PKGEOF'
package com.travelagency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ENTIDAD TRAVEL PACKAGE (Tabla "travel_packages" en la BD)
 * =========================================================
 * Representa un paquete turístico que la agencia vende.
 * Ejemplo: "Aventura en Machu Picchu - 5 días"
 *
 * Épica 2: Publicación y gestión de paquetes turísticos
 */
@Entity
@Table(name = "travel_packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre del paquete. Ej: "Aventura en Machu Picchu"
     */
    @Column(nullable = false)
    private String name;

    /**
     * Destino del viaje. Ej: "Cusco, Perú"
     */
    @Column(nullable = false)
    private String destination;

    /**
     * Descripción detallada del paquete.
     * @Lob = permite almacenar textos largos (más de 255 caracteres)
     */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Fecha de inicio del viaje.
     */
    @Column(nullable = false)
    private LocalDate startDate;

    /**
     * Fecha de término del viaje.
     * Regla: debe ser posterior a startDate
     */
    @Column(nullable = false)
    private LocalDate endDate;

    /**
     * Precio por persona en dólares.
     * BigDecimal se usa para dinero (más preciso que double)
     * Regla: debe ser mayor que cero
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Cantidad total de cupos disponibles.
     * Regla: debe ser mayor que cero
     */
    @Column(nullable = false)
    private Integer totalSlots;

    /**
     * Cupos que ya fueron reservados.
     * availableSlots = totalSlots - bookedSlots
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer bookedSlots = 0;

    /**
     * Servicios incluidos en el paquete.
     * Ej: "Hotel 4 estrellas, Desayuno, Transporte, Guía turístico"
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String includedServices;

    /**
     * Restricciones o condiciones del paquete.
     * Ej: "No incluye vuelos internacionales, Seguro de viaje obligatorio"
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String restrictions;

    /**
     * Tipo de viaje.
     * Ej: "Aventura", "Playa", "Cultural", "Familiar"
     */
    private String travelType;

    /**
     * Temporada del viaje.
     * Ej: "Alta", "Baja", "Media"
     */
    private String season;

    /**
     * Estado del paquete (controla su visibilidad y disponibilidad).
     * Valores posibles: AVAILABLE, SOLD_OUT, EXPIRED, CANCELLED
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PackageStatus status = PackageStatus.AVAILABLE;

    /**
     * Duración del viaje en días (se calcula automáticamente).
     */
    private Integer durationDays;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Calcular duración automáticamente
        if (startDate != null && endDate != null) {
            durationDays = (int) (endDate.toEpochDay() - startDate.toEpochDay());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (startDate != null && endDate != null) {
            durationDays = (int) (endDate.toEpochDay() - startDate.toEpochDay());
        }
    }

    /**
     * Método auxiliar: calcula cuántos cupos quedan disponibles.
     * Ej: si hay 20 cupos totales y 5 reservados, quedan 15.
     */
    public int getAvailableSlots() {
        return totalSlots - bookedSlots;
    }
}
PKGEOF

echo "   ✅ TravelPackage.java creado"

# ---- Enum PackageStatus ----
cat > backend/src/main/java/com/travelagency/entity/PackageStatus.java << 'STATUSEOF'
package com.travelagency.entity;

/**
 * ESTADOS POSIBLES DE UN PAQUETE TURÍSTICO
 * =========================================
 * Un enum (enumeración) define un conjunto fijo de valores.
 * Es como una lista cerrada: un paquete SOLO puede tener
 * uno de estos 4 estados.
 */
public enum PackageStatus {
    AVAILABLE,    // Disponible para reservar
    SOLD_OUT,     // Agotado (sin cupos)
    EXPIRED,      // No vigente (fecha ya pasó)
    CANCELLED     // Cancelado por la agencia
}
STATUSEOF

echo "   ✅ PackageStatus.java creado"

# ---- Entidad Booking ----
cat > backend/src/main/java/com/travelagency/entity/Booking.java << 'BOOKEOF'
package com.travelagency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ENTIDAD BOOKING (Tabla "bookings" en la BD)
 * ============================================
 * Representa una reserva hecha por un cliente.
 * Conecta un usuario con un paquete turístico.
 *
 * Épica 4: Proceso de reserva en línea
 */
@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * El cliente que hizo la reserva.
     * @ManyToOne = muchas reservas pueden pertenecer a un mismo usuario
     * @JoinColumn = la columna "user_id" en la tabla bookings
     *               referencia al id de la tabla users
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * El paquete turístico reservado.
     * @ManyToOne = muchas reservas pueden ser del mismo paquete
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private TravelPackage travelPackage;

    /**
     * Cantidad de pasajeros en esta reserva.
     * Regla: debe ser mayor que cero
     * Regla: no puede exceder los cupos disponibles del paquete
     */
    @Column(nullable = false)
    private Integer passengerCount;

    /**
     * Monto base (precio × pasajeros, SIN descuentos).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    /**
     * Porcentaje total de descuento aplicado.
     * Ej: 15.00 significa 15% de descuento
     * Regla: máximo 20%
     */
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    /**
     * Monto del descuento en dinero.
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Monto final a pagar (baseAmount - discountAmount).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Detalle de los descuentos aplicados.
     * Ej: "Descuento por grupo: 5%, Cliente frecuente: 10%"
     * Regla de transparencia: el cliente debe ver qué descuentos se aplicaron
     */
    @Column(columnDefinition = "TEXT")
    private String discountDetails;

    /**
     * Estado de la reserva.
     * Valores: PENDING, CONFIRMED, CANCELLED, EXPIRED
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
BOOKEOF

echo "   ✅ Booking.java creado"

# ---- Enum BookingStatus ----
cat > backend/src/main/java/com/travelagency/entity/BookingStatus.java << 'BSTATEOF'
package com.travelagency.entity;

/**
 * ESTADOS POSIBLES DE UNA RESERVA
 * ================================
 */
public enum BookingStatus {
    PENDING,     // Pendiente de pago
    CONFIRMED,   // Pagada y confirmada
    CANCELLED,   // Cancelada
    EXPIRED      // Expirada por falta de pago
}
BSTATEOF

echo "   ✅ BookingStatus.java creado"

# ---- Entidad Payment ----
cat > backend/src/main/java/com/travelagency/entity/Payment.java << 'PAYEOF'
package com.travelagency.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ENTIDAD PAYMENT (Tabla "payments" en la BD)
 * ============================================
 * Representa un pago realizado por un cliente.
 * Cada pago está asociado a una reserva.
 *
 * Épica 5: Gestión de pagos en línea
 *
 * IMPORTANTE: El pago es SIMULADO (no se conecta a ningún
 * banco real). Todo pago se asume como exitoso.
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * La reserva que se está pagando.
     * @OneToOne = cada reserva tiene exactamente un pago
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /**
     * Monto pagado (debe ser igual al totalAmount de la reserva).
     * Regla: no se permiten pagos parciales
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Medio de pago utilizado (siempre "CREDIT_CARD" en este proyecto).
     */
    @Column(nullable = false)
    private String paymentMethod;

    /**
     * Últimos 4 dígitos de la tarjeta (para referencia).
     * Ej: "4532" (no se guarda el número completo por seguridad)
     */
    private String cardLastFour;

    /**
     * Estado del pago (siempre "APPROVED" porque es simulado).
     */
    @Column(nullable = false)
    @Builder.Default
    private String paymentStatus = "APPROVED";

    /**
     * Fecha y hora en que se realizó el pago.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime paymentDate;

    @PrePersist
    protected void onCreate() {
        paymentDate = LocalDateTime.now();
    }
}
PAYEOF

echo "   ✅ Payment.java creado"

# ============================================================
# REPOSITORIOS (Acceso a Base de Datos - Capa Repository)
# ============================================================
echo "📦 Creando Repositorios..."

# ---- UserRepository ----
cat > backend/src/main/java/com/travelagency/repository/UserRepository.java << 'UREPOEOF'
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
UREPOEOF

echo "   ✅ UserRepository.java creado"

# ---- TravelPackageRepository ----
cat > backend/src/main/java/com/travelagency/repository/TravelPackageRepository.java << 'PREPOEOF'
package com.travelagency.repository;

import com.travelagency.entity.PackageStatus;
import com.travelagency.entity.TravelPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * REPOSITORIO DE PAQUETES TURÍSTICOS (Capa Repository)
 * =====================================================
 * Conecta con la tabla "travel_packages" en la BD.
 *
 * Épicas 2 y 3: Gestión y búsqueda de paquetes
 */
@Repository
public interface TravelPackageRepository extends JpaRepository<TravelPackage, Long> {

    /**
     * Busca todos los paquetes con un estado específico.
     * Ej: findByStatus(AVAILABLE) → todos los paquetes disponibles
     */
    List<TravelPackage> findByStatus(PackageStatus status);

    /**
     * Busca paquetes por destino (ignora mayúsculas/minúsculas).
     * "Containing" funciona como LIKE '%valor%' en SQL.
     * Ej: buscar "peru" encuentra "Cusco, Perú"
     */
    List<TravelPackage> findByDestinationContainingIgnoreCaseAndStatus(
            String destination, PackageStatus status);

    /**
     * Búsqueda avanzada con múltiples filtros usando JPQL.
     *
     * JPQL (Java Persistence Query Language) es como SQL
     * pero usa los nombres de las clases Java en vez de las tablas.
     *
     * :destination = parámetro que recibe el destino a buscar
     * :minPrice, :maxPrice = rango de precios
     * :startDate = fecha mínima de inicio del viaje
     *
     * La consulta filtra por:
     * 1. Solo paquetes DISPONIBLES
     * 2. Que el destino contenga el texto buscado (si se proporcionó)
     * 3. Que el precio esté dentro del rango (si se proporcionó)
     * 4. Que la fecha de inicio sea posterior a la indicada (si se proporcionó)
     */
    @Query("SELECT p FROM TravelPackage p WHERE p.status = 'AVAILABLE' " +
           "AND (:destination IS NULL OR LOWER(p.destination) LIKE LOWER(CONCAT('%', :destination, '%'))) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:startDate IS NULL OR p.startDate >= :startDate)")
    List<TravelPackage> searchPackages(
            @Param("destination") String destination,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("startDate") LocalDate startDate);
}
PREPOEOF

echo "   ✅ TravelPackageRepository.java creado"

# ---- BookingRepository ----
cat > backend/src/main/java/com/travelagency/repository/BookingRepository.java << 'BREPOEOF'
package com.travelagency.repository;

import com.travelagency.entity.Booking;
import com.travelagency.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REPOSITORIO DE RESERVAS (Capa Repository)
 * ==========================================
 * Conecta con la tabla "bookings" en la BD.
 *
 * Épicas 4, 6 y 7: Reservas, seguimiento y reportes
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Busca todas las reservas de un usuario específico.
     * Épica 6: el cliente solo ve SUS reservas.
     */
    List<Booking> findByUserId(Long userId);

    /**
     * Busca reservas de un usuario por estado.
     * Ej: ver solo las reservas confirmadas de un cliente
     */
    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status);

    /**
     * Cuenta cuántas reservas CONFIRMADAS tiene un usuario.
     * Se usa para determinar si es "cliente frecuente" (≥ 3 reservas pagadas).
     *
     * Épica 4: Descuento por cliente frecuente
     */
    long countByUserIdAndStatus(Long userId, BookingStatus status);

    /**
     * Busca reservas dentro de un rango de fechas.
     * Se usa para los reportes (Épica 7).
     *
     * Excluye reservas canceladas por defecto.
     */
    @Query("SELECT b FROM Booking b WHERE b.createdAt BETWEEN :startDate AND :endDate " +
           "AND b.status <> 'CANCELLED' ORDER BY b.createdAt DESC")
    List<Booking> findBookingsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Cuenta reservas de un usuario en un período (para descuento multi-paquete).
     */
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId " +
           "AND b.createdAt >= :since AND b.status <> 'CANCELLED'")
    long countRecentBookingsByUser(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);
}
BREPOEOF

echo "   ✅ BookingRepository.java creado"

# ---- PaymentRepository ----
cat > backend/src/main/java/com/travelagency/repository/PaymentRepository.java << 'PYREPOEOF'
package com.travelagency.repository;

import com.travelagency.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * REPOSITORIO DE PAGOS (Capa Repository)
 * ========================================
 * Conecta con la tabla "payments" en la BD.
 *
 * Épica 5: Gestión de pagos
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Busca el pago asociado a una reserva.
     */
    Optional<Payment> findByBookingId(Long bookingId);

    /**
     * Verifica si una reserva ya tiene un pago registrado.
     * Regla: solo se permite UN pago por reserva.
     */
    boolean existsByBookingId(Long bookingId);
}
PYREPOEOF

echo "   ✅ PaymentRepository.java creado"

# ============================================================
# DTOs (Data Transfer Objects - lo que envía/recibe la API)
# ============================================================
echo "📦 Creando DTOs..."

# ---- Request DTOs ----
cat > backend/src/main/java/com/travelagency/dto/request/CreatePackageRequest.java << 'CPREOF'
package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para CREAR un paquete turístico (lo que envía el frontend).
 *
 * DTO = Data Transfer Object
 * Es un objeto que transporta datos entre el frontend y el backend.
 * No se guarda en la BD, solo sirve para recibir/enviar información.
 *
 * Las anotaciones @NotBlank, @NotNull, @Positive validan
 * automáticamente que los datos sean correctos antes de procesarlos.
 */
@Data
public class CreatePackageRequest {

    @NotBlank(message = "Package name is required")
    private String name;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "Total slots is required")
    @Positive(message = "Total slots must be greater than zero")
    private Integer totalSlots;

    private String includedServices;
    private String restrictions;
    private String travelType;
    private String season;
}
CPREOF

cat > backend/src/main/java/com/travelagency/dto/request/CreateBookingRequest.java << 'CBREOF'
package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para CREAR una reserva.
 * El cliente envía el ID del paquete y cuántos pasajeros.
 */
@Data
public class CreateBookingRequest {

    @NotNull(message = "Package ID is required")
    private Long packageId;

    @NotNull(message = "Passenger count is required")
    @Positive(message = "Passenger count must be greater than zero")
    private Integer passengerCount;
}
CBREOF

cat > backend/src/main/java/com/travelagency/dto/request/PaymentRequest.java << 'PMTREOF'
package com.travelagency.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO para procesar un PAGO simulado.
 * Recibe datos de tarjeta de crédito ficticia.
 *
 * IMPORTANTE: estos datos NO se validan contra ningún banco real.
 * Todo pago se asume exitoso.
 */
@Data
public class PaymentRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "Expiration date is required")
    private String expirationDate;

    @NotBlank(message = "CVV is required")
    private String cvv;

    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;
}
PMTREOF

cat > backend/src/main/java/com/travelagency/dto/request/UserRegistrationRequest.java << 'UREOF'
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
UREOF

echo "   ✅ DTOs de Request creados"

# ============================================================
# EXCEPCIONES PERSONALIZADAS
# ============================================================
echo "📦 Creando Excepciones..."

cat > backend/src/main/java/com/travelagency/exception/ResourceNotFoundException.java << 'RNFEOF'
package com.travelagency.exception;

/**
 * Excepción que se lanza cuando no se encuentra un recurso.
 * Ej: buscar un paquete con ID 999 que no existe.
 *
 * Spring automáticamente devuelve un error 404 al frontend.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
RNFEOF

cat > backend/src/main/java/com/travelagency/exception/BusinessRuleException.java << 'BREOF'
package com.travelagency.exception;

/**
 * Excepción para reglas de negocio violadas.
 * Ej: intentar reservar más cupos de los disponibles.
 *
 * Spring devuelve un error 400 (Bad Request) al frontend.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
BREOF

cat > backend/src/main/java/com/travelagency/exception/GlobalExceptionHandler.java << 'GEHEOF'
package com.travelagency.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MANEJADOR GLOBAL DE EXCEPCIONES
 * ================================
 * Captura TODAS las excepciones del proyecto y las convierte
 * en respuestas HTTP amigables para el frontend.
 *
 * Sin esto, el frontend recibiría mensajes de error feos y técnicos.
 * Con esto, recibe mensajes claros como:
 *   { "error": "Package not found with id: 5" }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores cuando no se encuentra un recurso → 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Maneja errores de reglas de negocio → 400
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Maneja errores de validación (campos vacíos, email inválido, etc.) → 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("error", "Validation failed");
        body.put("details", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Método auxiliar que construye la respuesta de error.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
GEHEOF

echo "   ✅ Excepciones creadas"

# ============================================================
# SERVICIOS (Lógica de Negocio - Capa Service)
# ============================================================
echo "📦 Creando Servicios (Lógica de Negocio)..."

# ---- TravelPackageService ----
cat > backend/src/main/java/com/travelagency/service/TravelPackageService.java << 'TPSEOF'
package com.travelagency.service;

import com.travelagency.dto.request.CreatePackageRequest;
import com.travelagency.entity.PackageStatus;
import com.travelagency.entity.TravelPackage;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.TravelPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SERVICIO DE PAQUETES TURÍSTICOS (Capa Service)
 * ================================================
 * Contiene TODA la lógica de negocio relacionada con los
 * paquetes turísticos: crear, editar, buscar, validar.
 *
 * Épicas 2 y 3: Gestión y búsqueda de paquetes
 *
 * @Service = le dice a Spring que esta clase contiene lógica de negocio
 * @RequiredArgsConstructor = Lombok inyecta automáticamente las dependencias
 * @Transactional = las operaciones de BD se ejecutan dentro de una transacción
 *                  (si algo falla, se deshacen todos los cambios)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TravelPackageService {

    // Spring inyecta automáticamente el repositorio aquí
    private final TravelPackageRepository packageRepository;

    /**
     * CREAR un nuevo paquete turístico.
     *
     * Proceso:
     * 1. Validar que la fecha de término sea posterior a la de inicio
     * 2. Validar que el precio sea positivo
     * 3. Crear la entidad TravelPackage
     * 4. Guardar en la base de datos
     *
     * @param request datos del paquete enviados desde el frontend
     * @return el paquete creado con su ID generado
     * @throws BusinessRuleException si las fechas son inválidas
     */
    public TravelPackage createPackage(CreatePackageRequest request) {
        // Regla: la fecha de término debe ser posterior a la de inicio
        if (request.getEndDate().isBefore(request.getStartDate()) ||
            request.getEndDate().isEqual(request.getStartDate())) {
            throw new BusinessRuleException("End date must be after start date");
        }

        // Construir la entidad usando el patrón Builder
        TravelPackage travelPackage = TravelPackage.builder()
                .name(request.getName())
                .destination(request.getDestination())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .price(request.getPrice())
                .totalSlots(request.getTotalSlots())
                .bookedSlots(0)
                .includedServices(request.getIncludedServices())
                .restrictions(request.getRestrictions())
                .travelType(request.getTravelType())
                .season(request.getSeason())
                .status(PackageStatus.AVAILABLE)
                .build();

        // Guardar en la BD y retornar (el ID se genera automáticamente)
        return packageRepository.save(travelPackage);
    }

    /**
     * BUSCAR un paquete por su ID.
     *
     * @throws ResourceNotFoundException si el paquete no existe
     */
    @Transactional(readOnly = true)
    public TravelPackage getPackageById(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Package not found with id: " + id));
    }

    /**
     * OBTENER TODOS los paquetes (para administradores).
     */
    @Transactional(readOnly = true)
    public List<TravelPackage> getAllPackages() {
        return packageRepository.findAll();
    }

    /**
     * OBTENER solo los paquetes DISPONIBLES (para clientes).
     * Épica 3: solo mostrar paquetes reservables
     */
    @Transactional(readOnly = true)
    public List<TravelPackage> getAvailablePackages() {
        return packageRepository.findByStatus(PackageStatus.AVAILABLE);
    }

    /**
     * BUSCAR paquetes con filtros (Épica 3).
     *
     * Los parámetros son opcionales: si son null, no se aplica ese filtro.
     * Ej: searchPackages("Peru", null, null, null)
     *     → busca todos los paquetes disponibles con destino "Peru"
     */
    @Transactional(readOnly = true)
    public List<TravelPackage> searchPackages(String destination,
                                               BigDecimal minPrice,
                                               BigDecimal maxPrice,
                                               LocalDate startDate) {
        return packageRepository.searchPackages(destination, minPrice, maxPrice, startDate);
    }

    /**
     * ACTUALIZAR un paquete existente.
     *
     * Reglas:
     * - No se puede cambiar un paquete cancelado
     * - Las fechas deben seguir siendo válidas
     * - Si tiene reservas, no se pueden reducir cupos por debajo de lo reservado
     */
    public TravelPackage updatePackage(Long id, CreatePackageRequest request) {
        TravelPackage existing = getPackageById(id);

        // Regla: no modificar paquetes cancelados
        if (existing.getStatus() == PackageStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot update a cancelled package");
        }

        // Regla: fecha fin > fecha inicio
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessRuleException("End date must be after start date");
        }

        // Regla: no reducir cupos por debajo de lo ya reservado
        if (request.getTotalSlots() < existing.getBookedSlots()) {
            throw new BusinessRuleException(
                    "Cannot set total slots below already booked: " + existing.getBookedSlots());
        }

        // Actualizar los campos
        existing.setName(request.getName());
        existing.setDestination(request.getDestination());
        existing.setDescription(request.getDescription());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setPrice(request.getPrice());
        existing.setTotalSlots(request.getTotalSlots());
        existing.setIncludedServices(request.getIncludedServices());
        existing.setRestrictions(request.getRestrictions());
        existing.setTravelType(request.getTravelType());
        existing.setSeason(request.getSeason());

        return packageRepository.save(existing);
    }

    /**
     * CAMBIAR ESTADO de un paquete.
     *
     * Regla: un paquete con reservas no se puede eliminar,
     *        solo cambiar su estado.
     */
    public TravelPackage changeStatus(Long id, PackageStatus newStatus) {
        TravelPackage travelPackage = getPackageById(id);
        travelPackage.setStatus(newStatus);
        return packageRepository.save(travelPackage);
    }
}
TPSEOF

echo "   ✅ TravelPackageService.java creado"

# ---- BookingService ----
cat > backend/src/main/java/com/travelagency/service/BookingService.java << 'BSEOF'
package com.travelagency.service;

import com.travelagency.dto.request.CreateBookingRequest;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.TravelPackageRepository;
import com.travelagency.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICIO DE RESERVAS (Capa Service)
 * =====================================
 * Contiene la lógica de negocio más compleja del sistema:
 * crear reservas, calcular descuentos, validar cupos.
 *
 * Épica 4: Proceso de reserva en línea
 *
 * ESTA ES LA CLASE MÁS IMPORTANTE DEL PROYECTO.
 * Aquí se implementan las reglas de descuentos y validaciones.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TravelPackageRepository packageRepository;
    private final UserRepository userRepository;

    // ============================================================
    // CONSTANTES DE CONFIGURACIÓN DE DESCUENTOS
    // Estas son las reglas de negocio para los descuentos
    // ============================================================

    /** Mínimo de pasajeros para descuento por grupo */
    private static final int GROUP_DISCOUNT_THRESHOLD = 4;

    /** Porcentaje de descuento por grupo (5%) */
    private static final BigDecimal GROUP_DISCOUNT_PERCENT = new BigDecimal("5.00");

    /** Mínimo de reservas confirmadas para ser "cliente frecuente" */
    private static final int FREQUENT_CLIENT_THRESHOLD = 3;

    /** Porcentaje de descuento para cliente frecuente (10%) */
    private static final BigDecimal FREQUENT_DISCOUNT_PERCENT = new BigDecimal("10.00");

    /** Mínimo de reservas recientes para descuento multi-paquete */
    private static final int MULTI_PACKAGE_THRESHOLD = 1;

    /** Porcentaje de descuento por múltiples paquetes (3%) */
    private static final BigDecimal MULTI_PACKAGE_DISCOUNT_PERCENT = new BigDecimal("3.00");

    /** Máximo de descuento acumulable (20%) */
    private static final BigDecimal MAX_DISCOUNT_PERCENT = new BigDecimal("20.00");

    /**
     * CREAR UNA RESERVA
     * ===================
     * Este es el método principal. Proceso completo:
     *
     * 1. Verificar que el usuario existe
     * 2. Verificar que el paquete existe y está disponible
     * 3. Validar que hay cupos suficientes
     * 4. Calcular el precio base (precio × pasajeros)
     * 5. Calcular descuentos aplicables
     * 6. Crear la reserva con el monto final
     * 7. Descontar los cupos del paquete
     *
     * @param userId  ID del usuario que hace la reserva
     * @param request datos de la reserva (packageId + passengerCount)
     * @return la reserva creada
     */
    public Booking createBooking(Long userId, CreateBookingRequest request) {

        // PASO 1: Verificar que el usuario existe
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        // PASO 2: Verificar que el paquete existe
        TravelPackage travelPackage = packageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Package not found with id: " + request.getPackageId()));

        // PASO 3: Validar que el paquete esté disponible
        if (travelPackage.getStatus() != PackageStatus.AVAILABLE) {
            throw new BusinessRuleException(
                    "Package is not available for booking. Current status: " + travelPackage.getStatus());
        }

        // PASO 4: Validar que hay cupos suficientes
        int availableSlots = travelPackage.getAvailableSlots();
        if (request.getPassengerCount() > availableSlots) {
            throw new BusinessRuleException(
                    "Not enough slots. Requested: " + request.getPassengerCount()
                    + ", Available: " + availableSlots);
        }

        // PASO 5: Calcular precio base
        BigDecimal pricePerPerson = travelPackage.getPrice();
        BigDecimal baseAmount = pricePerPerson.multiply(
                BigDecimal.valueOf(request.getPassengerCount()));

        // PASO 6: Calcular descuentos
        DiscountResult discountResult = calculateDiscounts(
                userId, request.getPassengerCount(), baseAmount);

        // PASO 7: Calcular monto final
        BigDecimal totalAmount = baseAmount.subtract(discountResult.discountAmount);

        // Regla: el monto final nunca puede ser negativo
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // PASO 8: Crear la reserva
        Booking booking = Booking.builder()
                .user(user)
                .travelPackage(travelPackage)
                .passengerCount(request.getPassengerCount())
                .baseAmount(baseAmount)
                .discountPercentage(discountResult.totalPercentage)
                .discountAmount(discountResult.discountAmount)
                .totalAmount(totalAmount)
                .discountDetails(discountResult.details)
                .status(BookingStatus.PENDING)  // Siempre empieza como "pendiente"
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // PASO 9: Descontar cupos del paquete
        travelPackage.setBookedSlots(
                travelPackage.getBookedSlots() + request.getPassengerCount());

        // Si se agotaron los cupos, cambiar estado del paquete
        if (travelPackage.getAvailableSlots() <= 0) {
            travelPackage.setStatus(PackageStatus.SOLD_OUT);
        }

        packageRepository.save(travelPackage);

        return savedBooking;
    }

    /**
     * CALCULAR DESCUENTOS APLICABLES
     * ================================
     * Evalúa todas las reglas de descuento y determina cuáles aplican.
     *
     * Tipos de descuento:
     * 1. Por grupo (≥ 4 pasajeros): 5%
     * 2. Por cliente frecuente (≥ 3 reservas pagadas): 10%
     * 3. Por multi-paquete (otra reserva en los últimos 30 días): 3%
     *
     * Regla: los descuentos son ACUMULABLES pero con un TOPE de 20%
     */
    private DiscountResult calculateDiscounts(Long userId, int passengerCount, BigDecimal baseAmount) {

        BigDecimal totalPercentage = BigDecimal.ZERO;
        List<String> discountDescriptions = new ArrayList<>();

        // --- Descuento 1: Por grupo ---
        // Si viajan 4 o más personas, se aplica 5% de descuento
        if (passengerCount >= GROUP_DISCOUNT_THRESHOLD) {
            totalPercentage = totalPercentage.add(GROUP_DISCOUNT_PERCENT);
            discountDescriptions.add("Group discount (" + GROUP_DISCOUNT_THRESHOLD
                    + "+ passengers): " + GROUP_DISCOUNT_PERCENT + "%");
        }

        // --- Descuento 2: Cliente frecuente ---
        // Si el cliente tiene 3 o más reservas confirmadas (pagadas)
        long confirmedBookings = bookingRepository.countByUserIdAndStatus(
                userId, BookingStatus.CONFIRMED);
        if (confirmedBookings >= FREQUENT_CLIENT_THRESHOLD) {
            totalPercentage = totalPercentage.add(FREQUENT_DISCOUNT_PERCENT);
            discountDescriptions.add("Frequent client discount ("
                    + confirmedBookings + " confirmed bookings): "
                    + FREQUENT_DISCOUNT_PERCENT + "%");
        }

        // --- Descuento 3: Múltiples paquetes ---
        // Si el cliente hizo otra reserva en los últimos 30 días
        long recentBookings = bookingRepository.countRecentBookingsByUser(
                userId, LocalDateTime.now().minusDays(30));
        if (recentBookings >= MULTI_PACKAGE_THRESHOLD) {
            totalPercentage = totalPercentage.add(MULTI_PACKAGE_DISCOUNT_PERCENT);
            discountDescriptions.add("Multi-package discount: "
                    + MULTI_PACKAGE_DISCOUNT_PERCENT + "%");
        }

        // --- Aplicar tope máximo de 20% ---
        if (totalPercentage.compareTo(MAX_DISCOUNT_PERCENT) > 0) {
            totalPercentage = MAX_DISCOUNT_PERCENT;
            discountDescriptions.add("(Capped at maximum " + MAX_DISCOUNT_PERCENT + "%)");
        }

        // Calcular el monto del descuento en dinero
        BigDecimal discountAmount = baseAmount
                .multiply(totalPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        String details = discountDescriptions.isEmpty()
                ? "No discounts applied"
                : String.join(" | ", discountDescriptions);

        return new DiscountResult(totalPercentage, discountAmount, details);
    }

    /**
     * Clase interna para devolver el resultado de los descuentos.
     */
    private record DiscountResult(
            BigDecimal totalPercentage,
            BigDecimal discountAmount,
            String details) {}

    /**
     * OBTENER RESERVAS DE UN USUARIO (Épica 6)
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    /**
     * OBTENER TODAS LAS RESERVAS (para administradores, Épica 6)
     */
    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    /**
     * OBTENER UNA RESERVA POR ID
     */
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + id));
    }

    /**
     * CANCELAR UNA RESERVA
     * Al cancelar, se liberan los cupos del paquete.
     */
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Booking is already cancelled");
        }

        // Cambiar estado a cancelada
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Liberar cupos del paquete
        TravelPackage travelPackage = booking.getTravelPackage();
        travelPackage.setBookedSlots(
                travelPackage.getBookedSlots() - booking.getPassengerCount());

        // Si el paquete estaba agotado, vuelve a estar disponible
        if (travelPackage.getStatus() == PackageStatus.SOLD_OUT) {
            travelPackage.setStatus(PackageStatus.AVAILABLE);
        }

        packageRepository.save(travelPackage);

        return booking;
    }
}
BSEOF

echo "   ✅ BookingService.java creado"

# ---- PaymentService ----
cat > backend/src/main/java/com/travelagency/service/PaymentService.java << 'PSEOF'
package com.travelagency.service;

import com.travelagency.dto.request.PaymentRequest;
import com.travelagency.entity.*;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SERVICIO DE PAGOS (Capa Service)
 * ==================================
 * Procesa pagos simulados con tarjeta de crédito ficticia.
 *
 * Épica 5: Gestión de pagos en línea
 *
 * IMPORTANTE: Todo pago se asume como EXITOSO.
 * No hay conexión con bancos ni pasarelas reales.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    /**
     * PROCESAR UN PAGO
     * ==================
     * Proceso:
     * 1. Verificar que la reserva existe
     * 2. Verificar que no esté cancelada
     * 3. Verificar que no tenga un pago previo
     * 4. Registrar el pago (simulado, siempre exitoso)
     * 5. Cambiar el estado de la reserva a CONFIRMADA
     *
     * @param request datos de la tarjeta simulada
     * @return el pago registrado
     */
    public Payment processPayment(PaymentRequest request) {

        // PASO 1: Verificar que la reserva existe
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + request.getBookingId()));

        // PASO 2: No se puede pagar una reserva cancelada
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot pay for a cancelled booking");
        }

        // PASO 3: Solo un pago por reserva
        if (paymentRepository.existsByBookingId(request.getBookingId())) {
            throw new BusinessRuleException("Payment already exists for this booking");
        }

        // PASO 4: Obtener los últimos 4 dígitos de la tarjeta (para referencia)
        String cardLastFour = request.getCardNumber()
                .substring(request.getCardNumber().length() - 4);

        // PASO 5: Crear el registro de pago (simulado = siempre aprobado)
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalAmount())
                .paymentMethod("CREDIT_CARD")
                .cardLastFour(cardLastFour)
                .paymentStatus("APPROVED")
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // PASO 6: Cambiar estado de la reserva a CONFIRMADA
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return savedPayment;
    }

    /**
     * OBTENER PAGO DE UNA RESERVA
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for booking: " + bookingId));
    }
}
PSEOF

echo "   ✅ PaymentService.java creado"

# ============================================================
# CONTROLADORES (API REST - Capa Controller)
# ============================================================
echo "📦 Creando Controladores (API REST)..."

# ---- TravelPackageController ----
cat > backend/src/main/java/com/travelagency/controller/TravelPackageController.java << 'TPCEOF'
package com.travelagency.controller;

import com.travelagency.dto.request.CreatePackageRequest;
import com.travelagency.entity.PackageStatus;
import com.travelagency.entity.TravelPackage;
import com.travelagency.service.TravelPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CONTROLADOR DE PAQUETES TURÍSTICOS (Capa Controller)
 * =====================================================
 * Define los endpoints (URLs) de la API para paquetes.
 *
 * Ejemplo: cuando el frontend hace una petición GET a
 * http://localhost:8080/api/packages, este controlador
 * la recibe y la procesa.
 *
 * @RestController = indica que esta clase maneja peticiones HTTP
 *                   y devuelve datos (JSON), no páginas HTML
 * @RequestMapping = todas las URLs de este controlador empiezan con /api/packages
 * @CrossOrigin = permite que el frontend (puerto 3000) haga peticiones aquí
 */
@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TravelPackageController {

    private final TravelPackageService packageService;

    /**
     * CREAR un nuevo paquete turístico
     * URL: POST /api/packages
     * Acceso: solo ADMIN
     *
     * @Valid = activa las validaciones del DTO (campos obligatorios, etc.)
     * @RequestBody = lee los datos que vienen en el cuerpo de la petición (JSON)
     */
    @PostMapping
    public ResponseEntity<TravelPackage> createPackage(
            @Valid @RequestBody CreatePackageRequest request) {
        TravelPackage created = packageService.createPackage(request);
        // Retorna 201 CREATED con el paquete creado
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * OBTENER todos los paquetes (admin ve todos)
     * URL: GET /api/packages
     */
    @GetMapping
    public ResponseEntity<List<TravelPackage>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    /**
     * OBTENER solo paquetes disponibles (para clientes)
     * URL: GET /api/packages/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<TravelPackage>> getAvailablePackages() {
        return ResponseEntity.ok(packageService.getAvailablePackages());
    }

    /**
     * BUSCAR paquetes con filtros (Épica 3)
     * URL: GET /api/packages/search?destination=Peru&minPrice=500&maxPrice=2000
     *
     * @RequestParam = lee los parámetros de la URL
     * required = false: el parámetro es opcional
     */
    @GetMapping("/search")
    public ResponseEntity<List<TravelPackage>> searchPackages(
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) LocalDate startDate) {
        return ResponseEntity.ok(
                packageService.searchPackages(destination, minPrice, maxPrice, startDate));
    }

    /**
     * OBTENER un paquete por ID
     * URL: GET /api/packages/5
     *
     * @PathVariable = lee el ID de la URL
     */
    @GetMapping("/{id}")
    public ResponseEntity<TravelPackage> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.getPackageById(id));
    }

    /**
     * ACTUALIZAR un paquete existente
     * URL: PUT /api/packages/5
     * Acceso: solo ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<TravelPackage> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody CreatePackageRequest request) {
        return ResponseEntity.ok(packageService.updatePackage(id, request));
    }

    /**
     * CAMBIAR ESTADO de un paquete
     * URL: PATCH /api/packages/5/status?status=CANCELLED
     * Acceso: solo ADMIN
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TravelPackage> changeStatus(
            @PathVariable Long id,
            @RequestParam PackageStatus status) {
        return ResponseEntity.ok(packageService.changeStatus(id, status));
    }
}
TPCEOF

echo "   ✅ TravelPackageController.java creado"

# ---- BookingController ----
cat > backend/src/main/java/com/travelagency/controller/BookingController.java << 'BCEOF'
package com.travelagency.controller;

import com.travelagency.dto.request.CreateBookingRequest;
import com.travelagency.entity.Booking;
import com.travelagency.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * CONTROLADOR DE RESERVAS (Capa Controller)
 * ==========================================
 * Endpoints para crear, ver y cancelar reservas.
 *
 * Épicas 4 y 6: Reservas y seguimiento
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    /**
     * CREAR una nueva reserva
     * URL: POST /api/bookings?userId=1
     *
     * El userId se recibe como parámetro de la URL.
     * En producción con Keycloak, se obtendrá del token JWT.
     */
    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestParam Long userId,
            @Valid @RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    /**
     * OBTENER reservas de un usuario (Épica 6)
     * URL: GET /api/bookings/user/1
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getUserBookings(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    /**
     * OBTENER todas las reservas (admin, Épica 6)
     * URL: GET /api/bookings
     */
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    /**
     * OBTENER una reserva por ID
     * URL: GET /api/bookings/5
     */
    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    /**
     * CANCELAR una reserva
     * URL: PATCH /api/bookings/5/cancel
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}
BCEOF

echo "   ✅ BookingController.java creado"

# ---- PaymentController ----
cat > backend/src/main/java/com/travelagency/controller/PaymentController.java << 'PCEOF'
package com.travelagency.controller;

import com.travelagency.dto.request.PaymentRequest;
import com.travelagency.entity.Payment;
import com.travelagency.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CONTROLADOR DE PAGOS (Capa Controller)
 * ========================================
 * Endpoints para procesar pagos simulados.
 *
 * Épica 5: Gestión de pagos
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * PROCESAR un pago
     * URL: POST /api/payments
     */
    @PostMapping
    public ResponseEntity<Payment> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    /**
     * OBTENER pago de una reserva
     * URL: GET /api/payments/booking/5
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> getPaymentByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }
}
PCEOF

echo "   ✅ PaymentController.java creado"

# ============================================================
# CONFIGURACIÓN DE SEGURIDAD (temporalmente permisiva)
# ============================================================
cat > backend/src/main/java/com/travelagency/config/SecurityConfig.java << 'SECEOF'
package com.travelagency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * CONFIGURACIÓN DE SEGURIDAD
 * ============================
 * Por ahora permite TODAS las peticiones para facilitar el desarrollo.
 * Más adelante se integrará con Keycloak para autenticación real.
 *
 * NOTA: Esta configuración se reemplazará cuando integres Keycloak.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desactivar CSRF (no necesario en APIs REST)
            .csrf(csrf -> csrf.disable())
            // Configurar CORS (permitir peticiones del frontend)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Por ahora permitir TODAS las peticiones
            // TODO: Reemplazar con Keycloak
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * Configuración de CORS.
     * Permite que el frontend (React en puerto 3000) haga peticiones al backend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
SECEOF

echo "   ✅ SecurityConfig.java creado"

# ============================================================
# ARCHIVO .gitignore
# ============================================================
cat > backend/.gitignore << 'GITEOF'
target/
*.class
*.jar
*.war
.idea/
*.iml
.settings/
.project
.classpath
.vscode/
*.log
application-local.properties
GITEOF

