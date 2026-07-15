// Page Object de Login.js (backend/src/../pages/Login.js): formulario simple
// email + password, boton con texto "Ingresar" (className btn-search).
class LoginPage {
  constructor(page) {
    this.page = page;
    this.emailInput = page.locator('input[type="email"]');
    this.passwordInput = page.locator('input[type="password"]');
    this.submitButton = page.locator('button:has-text("Ingresar")');
    this.errorMessage = page.locator('.login-error');
  }

  async goto() {
    await this.page.goto('/login');
  }

  async login(email, password) {
    await this.goto();
    await this.emailInput.fill(email);
    await this.passwordInput.fill(password);
    await this.submitButton.click();
    // Login exitoso navega a "/" (PackageList)
    await this.page.waitForURL('/');
  }
}

module.exports = { LoginPage };
