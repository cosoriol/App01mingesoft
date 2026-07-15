// Criterios de aceptacion de Epica 5 (Pagos) -- Evaluacion 3.
// Cada test arma su propia reserva PENDING por API (para no interferir entre
// si) y ejecuta el GIVEN/WHEN/THEN contra la UI real de pago.
const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/LoginPage');
const { PaymentPage } = require('../pages/PaymentPage');
const { packages, users } = require('../fixtures/testData');
const helpers = require('../utils/helpers');

let pkgStandard;

test.describe('Epica 5 - Pagos', () => {
  test.beforeAll(async () => {
    const admin = await helpers.loginAdmin();
    pkgStandard = await helpers.ensurePackage(admin.id, packages.standard);

    await helpers.ensureUser(users.paymentClient1.fullName, users.paymentClient1.email);
    await helpers.ensureUser(users.paymentClient2.fullName, users.paymentClient2.email);
    await helpers.ensureUser(users.paymentClient3.fullName, users.paymentClient3.email);
  });

  test('Criterio 1: procesar pago exitoso y confirmar reserva', async ({ page }) => {
    const user = await helpers.ensureUser(users.paymentClient1.fullName, users.paymentClient1.email);

    // GIVEN: usuario con reserva PENDING valida (monto = total de la reserva)
    const booking = await helpers.createBookingViaApi(user.id, pkgStandard.id, 1);

    const login = new LoginPage(page);
    await login.login(users.paymentClient1.email, helpers.TEST_PASSWORD);
    await page.goto(`/payment/${booking.id}`);

    // WHEN: ingresa datos de tarjeta validos y confirma el pago
    const paymentPage = new PaymentPage(page);
    await paymentPage.fillCard({
      holderName: 'Playwright Pago Uno',
      cardNumber: '4111111111111111',
      expirationDate: '1229', // se auto-formatea a 12/29
      cvv: '123',
    });
    await paymentPage.pay();

    // THEN: el sistema registra el pago (APPROVED), la reserva pasa a CONFIRMED,
    // y se muestra el comprobante
    await page.waitForURL(new RegExp(`/payment-confirmation/${booking.id}`));
    await expect(page.locator('h1')).toContainText('¡Pago realizado exitosamente!');
    await expect(page.locator('body')).toContainText('Confirmada');
  });

  test('Criterio 2: rechazar tarjeta expirada', async ({ page }) => {
    const user = await helpers.ensureUser(users.paymentClient2.fullName, users.paymentClient2.email);
    const booking = await helpers.createBookingViaApi(user.id, pkgStandard.id, 1);

    const login = new LoginPage(page);
    await login.login(users.paymentClient2.email, helpers.TEST_PASSWORD);
    await page.goto(`/payment/${booking.id}`);

    // GIVEN/WHEN: intenta pagar con una tarjeta cuya fecha de expiracion ya paso
    const paymentPage = new PaymentPage(page);
    await paymentPage.fillCard({
      holderName: 'Playwright Pago Dos',
      cardNumber: '4111111111111111',
      expirationDate: '0120', // -> 01/20, Enero 2020: vencida
      cvv: '123',
    });
    await paymentPage.pay();

    // THEN: el sistema rechaza el pago y muestra el error de tarjeta expirada
    await expect(paymentPage.errorMessage).toContainText('expiration date has already passed');
    // La reserva sigue PENDING: el pago rechazado nunca debe confirmarla.
    await expect(page).toHaveURL(new RegExp(`/payment/${booking.id}`));
  });

  test('Criterio 3: validar formato de CVV de 3 digitos', async ({ page }) => {
    const user = await helpers.ensureUser(users.paymentClient3.fullName, users.paymentClient3.email);
    const booking = await helpers.createBookingViaApi(user.id, pkgStandard.id, 1);

    const login = new LoginPage(page);
    await login.login(users.paymentClient3.email, helpers.TEST_PASSWORD);
    await page.goto(`/payment/${booking.id}`);

    const paymentPage = new PaymentPage(page);

    // GIVEN/WHEN: intenta ingresar un CVV con formato invalido.
    // El propio campo filtra letras en cada tecla (solo deja digitos, ver
    // PaymentPage.js) -- eso ya es prevencion de errores (Heuristica 5); el
    // caso que SI llega al backend es un CVV incompleto (menos de 3 digitos).
    await paymentPage.fillCard({
      holderName: 'Playwright Pago Tres',
      cardNumber: '4111111111111111',
      expirationDate: '1229',
      cvv: '12',
    });
    await paymentPage.pay();

    // THEN: el sistema rechaza el pago y muestra el error de CVV invalido
    await expect(paymentPage.errorMessage).toContainText('CVV must be exactly 3 digits');
  });
});
