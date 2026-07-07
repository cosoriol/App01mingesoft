---
name: test-frontend-visually
description: Prueba visualmente el frontend de TravelAgency (React) en un Chromium headless vía Playwright — hace login con un usuario de prueba, navega a una ruta y toma un screenshot. Usar cuando se pide "probar en el navegador", "screenshot del frontend", "pushea esa página" o confirmar visualmente un cambio de UI en este proyecto.
---

# Probar el frontend de TravelAgency visualmente

Este proyecto es un monorepo: backend Spring Boot (`backend/`, puerto
8080) + frontend React con `react-scripts` (`frontend/`, puerto 3000).
`chromium-cli` no está disponible en este entorno, así que se maneja
un Chromium headless directamente con Playwright.

## 1. Prerequisitos: backend y frontend corriendo

```bash
# MySQL debe estar arriba (travelagency_db, root/root123)
service mysql status || sudo service mysql start

# Backend (puerto 8080) — desde backend/
cd backend && mvn spring-boot:run > /tmp/backend.log 2>&1 &
until curl -sf http://localhost:8080/api/packages/available >/dev/null; do sleep 1; done

# Frontend (puerto 3000) — desde frontend/
cd frontend && npm start > /tmp/frontend.log 2>&1 &
until curl -sf http://localhost:3000 >/dev/null; do sleep 1; done
```

Si el backend no está arriba, las páginas que dependen de datos (Mis
Reservas, detalle de paquete) van a cargar vacías o quedarse en
"Cargando..." — no es un bug del frontend, es falta del backend.

## 2. Setup de Playwright (una vez por entorno)

Si `node -e "require('playwright')"` falla, instalarlo en un
directorio de trabajo (no hace falta agregarlo como dependencia del
proyecto):

```bash
mkdir -p /tmp/pw-driver && cd /tmp/pw-driver
npm init -y >/dev/null 2>&1
npm install playwright
npx playwright install chromium   # descarga ~180MB, demora 1-2 min
```

## 3. Autenticación

Usuarios de prueba ya sembrados en la BD (todos con la misma
contraseña `password123`):

| Email | Rol |
|---|---|
| `ana.torres@example.com` | CLIENT |
| `bruno.rojas@example.com` | CLIENT |
| `admin@travelagency.com` | ADMIN |

El formulario de login está en `/login`: `input[type="email"]`,
`input[type="password"]`, `button[type="submit"]`. Señal de éxito:
aparece el texto `Cerrar Sesión` en la navbar.

## 4. Manejar el driver

Copia `drive.js` (en esta carpeta) al directorio donde instalaste
Playwright (o corre `node <ruta-a-esta-skill>/drive.js` si `playwright`
está resoluble desde ahí — más simple: copiarlo).

```bash
cp .claude/skills/test-frontend-visually/drive.js /tmp/pw-driver/
cd /tmp/pw-driver
node drive.js /my-bookings
# node drive.js /my-bookings bruno.rojas@example.com password123
# node drive.js /               (home, sin login: --no-login)
```

Imprime el texto de la página (para verificar contenido sin abrir la
imagen), errores de consola, y deja `screenshot-<ruta>.png` en el
directorio actual. **Mira el screenshot** — no asumas que "no tiró
error" significa que se ve bien.

## 5. Gotchas de este proyecto

- **Emoji sin renderizar (cuadros vacíos)**: el Chromium headless de
  este contenedor no tiene fuentes de emoji instaladas. Es cosmético
  del entorno de prueba, no un bug — en un navegador de escritorio
  normal (Chrome/Firefox) se ven bien. No lo reportes como hallazgo.
- **Inputs controlados de React**: usar `page.fill()` / `page.type()`,
  nunca `eval(el.value = ...)` — no dispara el `onChange` de React.
- **Primer render puede tardar**: `react-scripts` compila rutas on
  demand la primera vez; `waitForSelector` con timeout generoso
  (10s) en vez de un `sleep` fijo.
- **Sesión en localStorage**: el login persiste vía `localStorage`
  (`currentUser`), no cookies — cada `browser.newContext()` arranca
  sin sesión, hay que loguearse de nuevo en cada corrida del driver.
- **Datos de prueba se acumulan**: las reservas creadas en sesiones
  anteriores (PENDING, CANCELLED, EXPIRED) siguen apareciendo en
  "Mis Reservas" de los usuarios de prueba. Si necesitas ver un caso
  específico (ej. un descuento nuevo), crea una reserva fresca por
  `curl` antes de tomar el screenshot en vez de asumir que ya existe.
