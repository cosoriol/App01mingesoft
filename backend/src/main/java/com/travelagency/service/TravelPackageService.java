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
