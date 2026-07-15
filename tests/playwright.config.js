// Configuracion de Playwright para las pruebas funcionales de App01Mingesoft
// (Evaluacion 3: criterios de aceptacion Epica 4 y Epica 5, ver specs/).
const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './specs',
  timeout: 45000,
  fullyParallel: false, // los specs comparten datos de setup (usuario/paquetes de prueba)
  // 1 solo worker: esta maquina corre 18 contenedores de Docker (ambas apps) con muy
  // poca RAM libre: varios Chromium en paralelo generan timeouts erraticos por presion
  // de memoria, no por bugs reales -- ver README-DEPLOYMENT si esto se ejecuta en CI.
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'report' }]],
  use: {
    baseURL: 'http://localhost:3000',
    headless: true,
    screenshot: 'on',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
});
