// Page Object de BookingPage.js: elige cantidad de pasajeros y crea la
// reserva (boton "Continuar al pago"). Si falla, el error aparece en
// ".login-error" (validacion de cupos insuficientes, etc.).
class BookingPage {
  constructor(page) {
    this.page = page;
    this.passengerCountInput = page.locator('input[type="number"]');
    this.continueButton = page.locator('button:has-text("Continuar al pago")');
    this.errorMessage = page.locator('.login-error');
  }

  async setPassengerCount(count) {
    await this.passengerCountInput.fill(String(count));
  }

  async continueToPayment() {
    await this.continueButton.click();
  }
}

module.exports = { BookingPage };
