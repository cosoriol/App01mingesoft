#!/usr/bin/env node
/**
 * Driver de Playwright para probar visualmente el frontend de
 * TravelAgency: hace login, navega a una ruta y toma un screenshot.
 *
 * Uso:
 *   node drive.js [ruta] [email] [password]
 *
 * Ejemplos:
 *   node drive.js /my-bookings
 *   node drive.js /my-bookings bruno.rojas@example.com password123
 *   node drive.js /            (home, sin login)
 *
 * Requiere que el frontend (npm start, puerto 3000) y el backend
 * (mvn spring-boot:run, puerto 8080) ya estén corriendo. Ver SKILL.md.
 */
const { chromium } = require('playwright');

const FRONTEND_URL = process.env.FRONTEND_URL || 'http://localhost:3000';
const route = process.argv[2] || '/';
const email = process.argv[3] || 'ana.torres@example.com';
const password = process.argv[4] || 'password123';
const skipLogin = process.argv.includes('--no-login');

(async () => {
  const browser = await chromium.launch({ args: ['--no-sandbox'] });
  const page = await (await browser.newContext()).newPage();

  const errors = [];
  page.on('pageerror', (err) => errors.push(String(err)));
  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text());
  });

  if (!skipLogin) {
    console.log(`-> login como ${email}`);
    await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'networkidle' });
    await page.waitForSelector('text=Iniciar Sesión');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', password);
    await page.click('button[type="submit"]');
    await page.waitForSelector('text=Cerrar Sesión', { timeout: 10000 });
  }

  console.log(`-> nav ${route}`);
  await page.goto(`${FRONTEND_URL}${route}`, { waitUntil: 'networkidle' });
  // Dar tiempo a que los fetch de datos (axios) terminen y rendericen.
  await page.waitForTimeout(1500);

  const safeName = (route === '/' ? 'home' : route.replace(/[^\w]+/g, '_'));
  const shotPath = `screenshot-${safeName}.png`;
  await page.screenshot({ path: shotPath, fullPage: true });
  console.log(`-> screenshot guardado en ${shotPath}`);

  console.log('---- BODY TEXT ----');
  console.log(await page.innerText('body'));
  console.log('---- CONSOLE ERRORS ----');
  console.log(errors.length ? errors.join('\n') : '(ninguno)');

  await browser.close();
})();
