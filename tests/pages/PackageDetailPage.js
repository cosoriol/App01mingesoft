// Page Object de PackageDetail.js: muestra el detalle de un paquete y el
// boton "Reservar ahora" que lleva al flujo de reserva (Epica 4).
class PackageDetailPage {
  constructor(page) {
    this.page = page;
    this.reserveButton = page.locator('button:has-text("Reservar ahora")');
    this.availableSlots = page.locator('text=Cupos disponibles:').locator('..');
    this.notFoundOrSoldOut = page.locator('text=no tiene cupos disponibles');
  }

  async goto(packageId) {
    await this.page.goto(`/packages/${packageId}`);
    // PackageDetail.js hace un fetch async (useEffect) tras montarse: sin
    // esto, una lectura inmediata del body puede llegar antes de que React
    // termine de renderizar los datos del paquete.
    await this.page.locator('text=Cupos disponibles:').waitFor();
  }

  async getAvailableSlotsText() {
    // Aparece una sola vez en la pagina ("🎟️ Cupos disponibles: N"), asi que
    // buscar en todo el body es mas robusto que apuntar a un div especifico.
    const bodyText = await this.page.locator('body').innerText();
    const match = bodyText.match(/Cupos disponibles:\s*(\d+)/);
    return match ? Number(match[1]) : null;
  }

  async clickReserve() {
    await this.reserveButton.click();
  }
}

module.exports = { PackageDetailPage };
