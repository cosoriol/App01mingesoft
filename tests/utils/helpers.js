// Helpers que preparan datos de prueba llamando directamente a la API (via el
// balanceador, puerto 80) en vez de la UI. Esto es deliberado: los criterios de
// aceptacion se PRUEBAN con Playwright a traves de la interfaz real (eso es lo
// que exige la evaluacion), pero el ARRANGE (crear usuarios/paquetes/reservas
// previas necesarias para el escenario) es mas rapido y confiable por API.
// Node 18 trae fetch nativo, no hace falta ninguna dependencia extra.

const API_URL = 'http://localhost/api';

async function apiPost(path, body, query = {}) {
  const qs = new URLSearchParams(query).toString();
  const url = `${API_URL}${path}${qs ? `?${qs}` : ''}`;
  const res = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const data = await res.json().catch(() => ({}));
  return { status: res.status, data };
}

async function apiGet(path, query = {}) {
  const qs = new URLSearchParams(query).toString();
  const url = `${API_URL}${path}${qs ? `?${qs}` : ''}`;
  const res = await fetch(url);
  const data = await res.json().catch(() => ({}));
  return { status: res.status, data };
}

async function apiPatch(path, query = {}) {
  const qs = new URLSearchParams(query).toString();
  const url = `${API_URL}${path}${qs ? `?${qs}` : ''}`;
  const res = await fetch(url, { method: 'PATCH' });
  const data = await res.json().catch(() => ({}));
  return { status: res.status, data };
}

/** Password que cumple la regla real de fortaleza (mayus/minus/numero/especial de @#$%^&+=!*). */
const TEST_PASSWORD = 'Testing2026!';

/** Registra un usuario si no existe todavia; en cualquier caso, devuelve su sesion (login). */
async function ensureUser(fullName, email) {
  await apiPost('/auth/register', { fullName, email, password: TEST_PASSWORD });
  const { status, data } = await apiPost('/auth/login', { email, password: TEST_PASSWORD });
  if (status !== 200) {
    throw new Error(`No se pudo iniciar sesion como ${email}: ${JSON.stringify(data)}`);
  }
  return data.user; // { id, fullName, email, role, ... }
}

async function loginAdmin() {
  const { status, data } = await apiPost('/auth/login', {
    email: 'admin@travelagency.cl',
    password: 'Inside2009!',
  });
  if (status !== 200) {
    throw new Error(`No se pudo iniciar sesion como admin: ${JSON.stringify(data)}`);
  }
  return data.user;
}

/** Crea un paquete si no existe uno con ese nombre (idempotente entre corridas de la suite). */
async function ensurePackage(adminId, pkg) {
  const { data: existing } = await apiGet('/packages/available');
  const found = Array.isArray(existing) ? existing.find((p) => p.name === pkg.name) : null;
  if (found) return found;

  const { status, data } = await apiPost('/packages', pkg, { userId: adminId });
  if (status !== 201) {
    throw new Error(`No se pudo crear el paquete "${pkg.name}": ${JSON.stringify(data)}`);
  }
  return data;
}

async function createBookingViaApi(userId, packageId, passengerCount) {
  const { status, data } = await apiPost('/bookings', { packageId, passengerCount }, { userId });
  if (status !== 201) {
    throw new Error(`No se pudo crear la reserva: ${JSON.stringify(data)}`);
  }
  return data; // BookingResponse: { id, totalAmount, ... }
}

async function payBookingViaApi(userId, bookingId, amount) {
  const { status, data } = await apiPost(
    '/payments',
    {
      bookingId,
      amount,
      paymentMethod: 'CREDIT_CARD',
      cardNumber: '4111111111111111',
      expirationDate: '12/29',
      cvv: '123',
      cardHolderName: 'Playwright Test',
    },
    { userId },
  );
  if (status !== 201) {
    throw new Error(`No se pudo pagar la reserva ${bookingId}: ${JSON.stringify(data)}`);
  }
  return data;
}

/**
 * Deja al usuario en estado "cliente frecuente" (>= threshold reservas CONFIRMED),
 * reservando y pagando un paquete pequeno tantas veces como haga falta.
 */
async function ensureFrequentClient(userId, packageId, threshold = 3) {
  const { data: bookings } = await apiGet(`/bookings/user/${userId}`, { requesterId: userId, status: 'CONFIRMED' });
  let confirmedCount = Array.isArray(bookings) ? bookings.length : 0;

  while (confirmedCount < threshold) {
    const booking = await createBookingViaApi(userId, packageId, 1);
    await payBookingViaApi(userId, booking.id, booking.totalAmount);
    confirmedCount += 1;
  }
}

module.exports = {
  TEST_PASSWORD,
  ensureUser,
  loginAdmin,
  ensurePackage,
  createBookingViaApi,
  payBookingViaApi,
  ensureFrequentClient,
  apiGet,
  apiPatch,
};
