// Page Object de PaymentPage.js: formulario de tarjeta simulada. El numero de
// tarjeta y la fecha de expiracion se auto-formatean con espacios/"/" mientras
// se escribe (ver formatCardNumber/formatExpirationDate en el componente real);
// el CVV se filtra a solo digitos y maximo 3 caracteres en cada tecla.
class PaymentPage {
  constructor(page) {
    this.page = page;
    this.cardHolderInput = page.locator('input[placeholder="Como aparece en la tarjeta"]');
    this.cardNumberInput = page.locator('input.card-number-input');
    this.expirationInput = page.locator('input[placeholder="MM/YY"]');
    this.cvvInput = page.locator('input[placeholder="123"]');
    this.payButton = page.locator('button.btn-pay');
    this.errorMessage = page.locator('.login-error');
    this.totalAmount = page.locator('.payment-summary-total');
  }

  async fillCard({ holderName, cardNumber, expirationDate, cvv }) {
    if (holderName !== undefined) await this.cardHolderInput.fill(holderName);
    if (cardNumber !== undefined) await this.cardNumberInput.fill(cardNumber);
    if (expirationDate !== undefined) await this.expirationInput.fill(expirationDate);
    if (cvv !== undefined) await this.cvvInput.fill(cvv);
  }

  async pay() {
    await this.payButton.click();
  }
}

module.exports = { PaymentPage };
