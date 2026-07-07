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
