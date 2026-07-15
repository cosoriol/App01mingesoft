// Criterios de aceptacion de Epica 4 (Reservas) -- Evaluacion 3.
// El arrange (usuarios, paquetes, historial de reservas previas) se prepara
// por API en beforeAll (ver utils/helpers.js); el GIVEN/WHEN/THEN de cada
// criterio se ejecuta contra la UI real con Playwright.
const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/LoginPage');
const { PackageDetailPage } = require('../pages/PackageDetailPage');
const { BookingPage } = require('../pages/BookingPage');
const { packages, users } = require('../fixtures/testData');
const helpers = require('../utils/helpers');

let pkgStandard;
let pkgFrequentHistory;
let pkgLowSlots;

test.describe('Epica 4 - Reservas', () => {
  test.beforeAll(async () => {
    const admin = await helpers.loginAdmin();

    pkgStandard = await helpers.ensurePackage(admin.id, packages.standard);
    pkgFrequentHistory = await helpers.ensurePackage(admin.id, packages.cheapForFrequentHistory);
    pkgLowSlots = await helpers.ensurePackage(admin.id, packages.lowSlots);

    await helpers.ensureUser(users.client1.fullName, users.client1.email);
    const frequentUser = await helpers.ensureUser(users.frequentClient.fullName, users.frequentClient.email);
    await helpers.ensureUser(users.client3.fullName, users.client3.email);

    // Precondicion del Criterio 2: el cliente ya debe tener >= 3 reservas CONFIRMED
    // ANTES del escenario que se prueba por UI.
    await helpers.ensureFrequentClient(frequentUser.id, pkgFrequentHistory.id, 3);
  });

  test('Criterio 1: crear reserva exitosa con validaciones', async ({ page }) => {
    // GIVEN: usuario autenticado en la plataforma
    const login = new LoginPage(page);
    await login.login(users.client1.email, helpers.TEST_PASSWORD);

    const detail = new PackageDetailPage(page);
    await detail.goto(pkgStandard.id);
    const slotsBefore = await detail.getAvailableSlotsText();

    // WHEN: selecciona un paquete disponible y completa la reserva con datos validos
    await detail.clickReserve();
    await page.waitForURL(new RegExp(`/booking/${pkgStandard.id}`));
    const booking = new BookingPage(page);
    await booking.setPassengerCount(2);
    await booking.continueToPayment();

    // THEN: el sistema crea la reserva (avanza a la pantalla de pago) ...
    await page.waitForURL(/\/payment\//);
    await expect(page.locator('h1')).toContainText('Confirmar Pago');

    // ... y decrementa los cupos disponibles del paquete
    await detail.goto(pkgStandard.id);
    const slotsAfter = await detail.getAvailableSlotsText();
    expect(slotsAfter).toBe(slotsBefore - 2);
  });

  test('Criterio 2: aplicar descuento de cliente frecuente automaticamente', async ({ page }) => {
    // GIVEN: el usuario es cliente frecuente (>= 3 reservas CONFIRMED, armado en beforeAll)
    const login = new LoginPage(page);
    await login.login(users.frequentClient.email, helpers.TEST_PASSWORD);

    // WHEN: realiza una nueva reserva
    const detail = new PackageDetailPage(page);
    await detail.goto(pkgStandard.id);
    await detail.clickReserve();
    await page.waitForURL(new RegExp(`/booking/${pkgStandard.id}`));
    const booking = new BookingPage(page);
    await booking.setPassengerCount(1);
    await booking.continueToPayment();
    await page.waitForURL(/\/payment\//);

    // THEN: el sistema aplica automaticamente el descuento por cliente frecuente (10%),
    // visible en el resumen de pago antes de ingresar la tarjeta
    await expect(page.locator('body')).toContainText('Cliente frecuente');
    await expect(page.locator('body')).toContainText('10%');
  });

  test('Criterio 3: validar restricciones de cupos disponibles', async ({ page }) => {
    // GIVEN: el paquete tiene solo 2 cupos disponibles
    const login = new LoginPage(page);
    await login.login(users.client3.email, helpers.TEST_PASSWORD);

    const detail = new PackageDetailPage(page);
    await detail.goto(pkgLowSlots.id);
    const slotsBefore = await detail.getAvailableSlotsText();
    expect(slotsBefore).toBe(2);

    // WHEN: el usuario intenta reservar 5 pasajeros
    await detail.clickReserve();
    await page.waitForURL(new RegExp(`/booking/${pkgLowSlots.id}`));
    const booking = new BookingPage(page);
    await booking.setPassengerCount(5);

    // THEN: el sistema rechaza la reserva. La UI la bloquea en el propio
    // formulario (Heuristica 5 de Nielsen: prevencion de errores, ver
    // BookingPage.js "validCount") en vez de esperar el rechazo del backend:
    // el boton queda deshabilitado y se muestra el mensaje de cupos.
    await expect(booking.errorMessage).toContainText(`entre 1 y ${slotsBefore}`);
    await expect(booking.continueButton).toBeDisabled();
  });
});
