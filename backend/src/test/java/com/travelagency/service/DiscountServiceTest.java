package com.travelagency.service;

import com.travelagency.config.DiscountConfig;
import com.travelagency.dto.response.DiscountDetail;
import com.travelagency.dto.response.DiscountType;
import com.travelagency.entity.BookingStatus;
import com.travelagency.entity.Promotion;
import com.travelagency.repository.BookingRepository;
import com.travelagency.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * TESTS UNITARIOS DE DiscountService (Épica 4: Reglas comerciales de descuentos)
 * ==================================================================================
 * Aquí vive la verificación de la matemática REAL de los descuentos:
 * cada regla por separado, la acumulación, el tope máximo, las
 * promociones vigentes, y que el desglose (JSON) se pueda ir y volver
 * sin perder información. Cada regla tiene al menos un caso positivo
 * y uno negativo.
 *
 * DiscountConfig se instancia directamente (no se mockea) con los
 * MISMOS valores por defecto de application.properties, para que los
 * tests reflejen el comportamiento real configurado.
 */
@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @InjectMocks
    private DiscountService discountService;

    private DiscountConfig discountConfig;

    /**
     * DiscountConfig no se puede crear con @InjectMocks porque no es
     * un mock: es la configuración real. Se arma a mano con los
     * mismos valores por defecto de application.properties y se
     * inyecta manualmente en el campo privado del servicio.
     */
    @BeforeEach
    void setUp() throws Exception {
        discountConfig = new DiscountConfig();
        discountConfig.getGroup().setThreshold(4);
        discountConfig.getGroup().setPercentage(new BigDecimal("5.0"));
        discountConfig.getFrequent().setThreshold(3);
        discountConfig.getFrequent().setPercentage(new BigDecimal("10.0"));
        discountConfig.getMultipackage().setDays(30);
        discountConfig.getMultipackage().setPercentage(new BigDecimal("3.0"));
        discountConfig.getMax().setPercentage(new BigDecimal("20.0"));

        java.lang.reflect.Field field = DiscountService.class.getDeclaredField("discountConfig");
        field.setAccessible(true);
        field.set(discountService, discountConfig);

        // Por defecto: sin reservas previas, sin promociones vigentes.
        // Cada test sobreescribe lo que necesite. lenient() porque no
        // todos los tests usan las 3 reglas a la vez (Mockito, con
        // stubbing estricto, marcaría como error un stub no usado).
        lenient().when(bookingRepository.countByUserIdAndStatus(anyLong(), any(BookingStatus.class))).thenReturn(0L);
        lenient().when(bookingRepository.countRecentBookingsByUser(anyLong(), any(LocalDateTime.class))).thenReturn(0L);
        lenient().when(promotionRepository.findActivePromotions(any(LocalDate.class))).thenReturn(List.of());
    }

    // ============================================================
    // REGLA 1: DESCUENTO POR CANTIDAD DE PASAJEROS (GRUPO)
    // ============================================================

    @Test
    void regla1_positivo_pasajerosEnUmbral_aplicaDescuentoDeGrupo() {
        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 4, new BigDecimal("400.00"));

        assertEquals(0, new BigDecimal("5.0").compareTo(result.totalPercentage()));
        assertEquals(0, new BigDecimal("20.00").compareTo(result.discountAmount())); // 5% de 400
        assertTrue(result.breakdown().stream().anyMatch(d -> d.getType() == DiscountType.GROUP));
    }

    @Test
    void regla1_negativo_pasajerosBajoElUmbral_noAplicaDescuentoDeGrupo() {
        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 3, new BigDecimal("300.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalPercentage()));
        assertTrue(result.breakdown().stream().noneMatch(d -> d.getType() == DiscountType.GROUP));
    }

    // ============================================================
    // REGLA 2: DESCUENTO POR CLIENTE FRECUENTE
    // ============================================================

    @Test
    void regla2_positivo_clienteConReservasConfirmadasEnElUmbral_aplicaDescuento() {
        when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(3L);

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("10.0").compareTo(result.totalPercentage()));
        assertTrue(result.breakdown().stream().anyMatch(d -> d.getType() == DiscountType.FREQUENT_CLIENT));
    }

    @Test
    void regla2_negativo_clienteConPocasReservasConfirmadas_noAplicaDescuento() {
        when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(2L);

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("100.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalPercentage()));
        assertTrue(result.breakdown().stream().noneMatch(d -> d.getType() == DiscountType.FREQUENT_CLIENT));
    }

    // ============================================================
    // REGLA 3: DESCUENTO POR COMPRA DE MÚLTIPLES PAQUETES
    // ============================================================

    @Test
    void regla3_positivo_otraReservaReciente_aplicaDescuentoMultiPaquete() {
        when(bookingRepository.countRecentBookingsByUser(anyLong(), any(LocalDateTime.class))).thenReturn(1L);

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("3.0").compareTo(result.totalPercentage()));
        assertTrue(result.breakdown().stream().anyMatch(d -> d.getType() == DiscountType.MULTI_PACKAGE));
    }

    @Test
    void regla3_negativo_sinReservasRecientes_noAplicaDescuentoMultiPaquete() {
        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("100.00"));

        assertEquals(0, BigDecimal.ZERO.compareTo(result.totalPercentage()));
        assertTrue(result.breakdown().stream().noneMatch(d -> d.getType() == DiscountType.MULTI_PACKAGE));
    }

    // ============================================================
    // REGLA 4: ACUMULACIÓN Y TOPE MÁXIMO
    // ============================================================

    @Test
    void regla4_positivo_descuentosAcumulablesBajoElTope_seSuman() {
        // Grupo (5%) + cliente frecuente (10%) = 15%, bajo el tope de 20%
        when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(5L);

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 4, new BigDecimal("400.00"));

        assertEquals(0, new BigDecimal("15.0").compareTo(result.totalPercentage()));
    }

    @Test
    void regla4_negativo_sumaSuperaElTope_seRecortaAlMaximo() {
        // Grupo (5%) + cliente frecuente (10%) + multi-paquete (3%) + promo (5%) = 23%
        when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(5L);
        when(bookingRepository.countRecentBookingsByUser(anyLong(), any(LocalDateTime.class))).thenReturn(1L);
        when(promotionRepository.findActivePromotions(any(LocalDate.class))).thenReturn(
                List.of(Promotion.builder()
                        .id(1L).name("Verano 2026")
                        .discountPercentage(new BigDecimal("5.0"))
                        .startDate(LocalDate.now().minusDays(1))
                        .endDate(LocalDate.now().plusDays(1))
                        .active(true)
                        .build()));

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 4, new BigDecimal("400.00"));

        // 23% calculado, pero se recorta a 20% (el tope configurado)
        assertEquals(0, new BigDecimal("20.0").compareTo(result.totalPercentage()));
        assertEquals(0, new BigDecimal("80.00").compareTo(result.discountAmount())); // 20% de 400
    }

    @Test
    void regla4_negativo_montoFinalNuncaEsNegativo() {
        when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(10L);

        // baseAmount muy pequeño: aunque el % de descuento sea alto,
        // el resultado nunca debe bajar de cero.
        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("0.01"));

        assertTrue(result.totalAmount().compareTo(BigDecimal.ZERO) >= 0);
    }

    // ============================================================
    // REGLA 5: PROMOCIONES POR TIEMPO LIMITADO
    // ============================================================

    @Test
    void regla5_positivo_promocionVigente_seSumaAlDescuento() {
        Promotion promotion = Promotion.builder()
                .id(1L).name("Verano 2026")
                .discountPercentage(new BigDecimal("7.0"))
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().plusDays(2))
                .active(true)
                .build();
        when(promotionRepository.findActivePromotions(any(LocalDate.class))).thenReturn(List.of(promotion));

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("7.0").compareTo(result.totalPercentage()));
        DiscountDetail detail = result.breakdown().get(0);
        assertEquals(DiscountType.PROMOTION, detail.getType());
        assertTrue(detail.getDescription().contains("Verano 2026"));
    }

    @Test
    void regla5_negativo_sinPromocionesVigentes_noSumaNada() {
        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("100.00"));

        assertTrue(result.breakdown().stream().noneMatch(d -> d.getType() == DiscountType.PROMOTION));
    }

    // ============================================================
    // REGLA 6: ORDEN Y CÁLCULO DEL MONTO FINAL
    // ============================================================

    @Test
    void regla6_positivo_totalAmountEsBaseAmountMenosDescuento() {
        when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(3L);

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("200.00"));

        // 10% de descuento sobre 200.00 = 20.00 -> total 180.00
        assertEquals(0, new BigDecimal("20.00").compareTo(result.discountAmount()));
        assertEquals(0, new BigDecimal("180.00").compareTo(result.totalAmount()));
    }

    // ============================================================
    // REGLA 7: TRANSPARENCIA (desglose serializado y reconstruible)
    // ============================================================

    @Test
    void regla7_positivo_desgloseSePuedeSerializarYReconstruirSinPerderInformacion() {
        when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(5L);

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 4, new BigDecimal("400.00"));

        // El JSON persistido debe reconstruir EXACTAMENTE el mismo desglose
        List<DiscountDetail> reconstructed = DiscountDetail.fromJson(result.discountDetailsJson());

        assertEquals(result.breakdown().size(), reconstructed.size());
        assertTrue(reconstructed.stream().anyMatch(d -> d.getType() == DiscountType.GROUP));
        assertTrue(reconstructed.stream().anyMatch(d -> d.getType() == DiscountType.FREQUENT_CLIENT));
    }

    @Test
    void regla7_negativo_jsonVacioOInvalido_devuelveListaVaciaSinLanzarExcepcion() {
        assertTrue(DiscountDetail.fromJson(null).isEmpty());
        assertTrue(DiscountDetail.fromJson("").isEmpty());
        assertTrue(DiscountDetail.fromJson("esto no es JSON").isEmpty());
    }

    @Test
    void regla7_positivo_discountSummaryEsTextoLegibleConDescripcionesYTope() {
        when(bookingRepository.countByUserIdAndStatus(1L, BookingStatus.CONFIRMED)).thenReturn(5L);

        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 4, new BigDecimal("400.00"));

        String summary = result.discountSummary();

        assertTrue(summary.contains("Descuento por grupo (4+ pasajeros): 5.0%"));
        assertTrue(summary.contains("Cliente frecuente (5 reservas confirmadas): 10.0%"));
        assertTrue(summary.contains("Total: 15.0% (tope: 20.0%)"));
        // Los ítems van separados por " | " para que sea fácil de leer de un vistazo
        assertTrue(summary.contains(" | "));
    }

    @Test
    void regla7_negativo_sinDescuentos_discountSummaryLoIndicaClaramente() {
        DiscountCalculationResult result = discountService.calculateDiscounts(
                1L, 1, new BigDecimal("100.00"));

        assertEquals("No se aplicaron descuentos", result.discountSummary());
    }
}
