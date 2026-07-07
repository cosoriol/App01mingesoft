package com.travelagency.service;

import com.travelagency.dto.request.CreatePackageRequest;
import com.travelagency.entity.PackageStatus;
import com.travelagency.entity.TravelPackage;
import com.travelagency.entity.User;
import com.travelagency.exception.BusinessRuleException;
import com.travelagency.exception.ResourceNotFoundException;
import com.travelagency.repository.TravelPackageRepository;
import com.travelagency.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TESTS UNITARIOS DE TravelPackageService (Épica 2: Publicación de paquetes)
 * ==============================================================================
 * Cubre la corrección del bug de autorización (crear/editar/cambiar
 * estado solo lo puede hacer un ADMIN) y las reglas de negocio ya
 * existentes. Se mockean los repositorios.
 */
@ExtendWith(MockitoExtension.class)
class TravelPackageServiceTest {

    @Mock
    private TravelPackageRepository packageRepository;

    @Mock
    private UserRepository userRepository;

    private TravelPackageService packageService;

    /**
     * TravelPackageService depende de AccessControlService (que usa
     * UserRepository). Se construye una instancia REAL apoyada en el
     * mismo userRepository ya mockeado, en vez de mockear
     * AccessControlService — así los tests de "solo ADMIN" siguen
     * funcionando igual que antes del refactor.
     */
    @BeforeEach
    void setUp() {
        AccessControlService accessControlService = new AccessControlService(userRepository);
        packageService = new TravelPackageService(packageRepository, accessControlService);
    }

    private User buildUser(Long id, String role) {
        return User.builder().id(id).fullName("Usuario " + id).email("u" + id + "@example.com")
                .role(role).active(true).build();
    }

    private CreatePackageRequest buildValidRequest() {
        CreatePackageRequest request = new CreatePackageRequest();
        request.setName("Aventura en Machu Picchu");
        request.setDestination("Cusco, Perú");
        request.setDescription("Descripción de prueba");
        request.setStartDate(LocalDate.now().plusDays(10));
        request.setEndDate(LocalDate.now().plusDays(15));
        request.setPrice(new BigDecimal("890.00"));
        request.setTotalSlots(20);
        return request;
    }

    private TravelPackage buildPackage(Long id, PackageStatus status, int totalSlots, int bookedSlots) {
        return TravelPackage.builder()
                .id(id).name("Paquete existente").destination("Destino")
                .description("Desc").startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(10)).price(new BigDecimal("500.00"))
                .totalSlots(totalSlots).bookedSlots(bookedSlots).status(status)
                .build();
    }

    // ============================================================
    // AUTORIZACIÓN: SOLO ADMIN CREA/EDITA/CAMBIA ESTADO (bug fix)
    // ============================================================

    @Test
    void autorizacion_positivo_adminPuedeCrearUnPaquete() {
        User admin = buildUser(1L, "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> packageService.createPackage(1L, buildValidRequest()));
    }

    @Test
    void autorizacion_negativo_clienteNoPuedeCrearUnPaquete() {
        User client = buildUser(2L, "CLIENT");
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> packageService.createPackage(2L, buildValidRequest()));

        assertTrue(ex.getMessage().contains("ADMIN"));
        verify(packageRepository, never()).save(any());
    }

    @Test
    void autorizacion_negativo_usuarioInexistente_lanzaResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> packageService.createPackage(99L, buildValidRequest()));
    }

    @Test
    void autorizacion_negativo_clienteNoPuedeEditarUnPaquete() {
        User client = buildUser(2L, "CLIENT");
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class,
                () -> packageService.updatePackage(2L, 1L, buildValidRequest()));
    }

    @Test
    void autorizacion_negativo_clienteNoPuedeCambiarElEstado() {
        User client = buildUser(2L, "CLIENT");
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));

        assertThrows(BusinessRuleException.class,
                () -> packageService.changeStatus(2L, 1L, PackageStatus.CANCELLED));
    }

    @Test
    void autorizacion_positivo_adminPuedeCambiarElEstado() {
        User admin = buildUser(1L, "ADMIN");
        TravelPackage existing = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(inv -> inv.getArgument(0));

        TravelPackage result = packageService.changeStatus(1L, 1L, PackageStatus.CANCELLED);

        assertEquals(PackageStatus.CANCELLED, result.getStatus());
    }

    // ============================================================
    // REGLAS DE NEGOCIO YA EXISTENTES (siguen intactas tras el fix)
    // ============================================================

    @Test
    void reglaFechas_negativo_fechaFinNoEsPosteriorAFechaInicio_lanzaBusinessRuleException() {
        User admin = buildUser(1L, "ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        CreatePackageRequest request = buildValidRequest();
        request.setEndDate(request.getStartDate());

        assertThrows(BusinessRuleException.class, () -> packageService.createPackage(1L, request));
    }

    @Test
    void reglaActualizar_negativo_noSePuedeEditarUnPaqueteCancelado() {
        User admin = buildUser(1L, "ADMIN");
        TravelPackage cancelled = buildPackage(1L, PackageStatus.CANCELLED, 10, 0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(cancelled));

        assertThrows(BusinessRuleException.class,
                () -> packageService.updatePackage(1L, 1L, buildValidRequest()));
    }

    @Test
    void reglaActualizar_negativo_noSePuedeReducirCuposPorDebajoDeLoReservado() {
        User admin = buildUser(1L, "ADMIN");
        TravelPackage existing = buildPackage(1L, PackageStatus.AVAILABLE, 10, 8);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(existing));

        CreatePackageRequest request = buildValidRequest();
        request.setTotalSlots(5); // menos que los 8 ya reservados

        assertThrows(BusinessRuleException.class, () -> packageService.updatePackage(1L, 1L, request));
    }

    @Test
    void reglaActualizar_positivo_adminPuedeEditarUnPaqueteDisponible() {
        User admin = buildUser(1L, "ADMIN");
        TravelPackage existing = buildPackage(1L, PackageStatus.AVAILABLE, 10, 2);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(packageRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(packageRepository.save(any(TravelPackage.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePackageRequest request = buildValidRequest();
        request.setName("Nombre actualizado");

        TravelPackage result = packageService.updatePackage(1L, 1L, request);

        assertEquals("Nombre actualizado", result.getName());
    }

    // ============================================================
    // BÚSQUEDA CON FILTROS (Épica 3)
    // ============================================================

    @Test
    void busqueda_positivo_delegaLosFiltrosAlRepositorio() {
        TravelPackage match = buildPackage(1L, PackageStatus.AVAILABLE, 10, 0);
        when(packageRepository.searchPackages("Peru", null, null, null, "Aventura", "Alta"))
                .thenReturn(List.of(match));

        List<TravelPackage> result = packageService.searchPackages(
                "Peru", null, null, null, "Aventura", "Alta");

        assertEquals(1, result.size());
    }

    @Test
    void busqueda_negativo_sinCoincidencias_devuelveListaVacia() {
        when(packageRepository.searchPackages(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        List<TravelPackage> result = packageService.searchPackages(
                "Antártida", null, null, null, null, null);

        assertTrue(result.isEmpty());
    }

    // ============================================================
    // LISTAR TODOS LOS PAQUETES (solo ADMIN) — corrección de bug
    // ============================================================

    @Test
    void listarTodos_positivo_adminPuedeVerTodosLosPaquetesIncluidosLosCancelados() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, "ADMIN")));
        when(packageRepository.findAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> packageService.getAllPackages(1L));
        verify(packageRepository).findAll();
    }

    @Test
    void listarTodos_negativo_clienteNoPuedeListarTodosLosPaquetes() {
        // Corrección de bug: antes cualquiera podía ver paquetes
        // cancelados/agotados que ya no están en el catálogo público.
        when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, "CLIENT")));

        assertThrows(BusinessRuleException.class, () -> packageService.getAllPackages(2L));
        verify(packageRepository, never()).findAll();
    }
}
