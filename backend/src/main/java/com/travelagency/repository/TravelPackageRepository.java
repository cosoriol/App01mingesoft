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
