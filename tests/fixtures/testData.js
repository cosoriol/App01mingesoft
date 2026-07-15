// Datos de prueba fijos para los criterios de aceptacion de Epica 4 y Epica 5.
// Los paquetes se crean (o reutilizan, si ya existen de una corrida anterior)
// via API en el arrange de cada spec -- ver utils/helpers.js.

const packages = {
  // Criterio 1 (Epica 4): reserva exitosa simple. Muchos cupos: el test nunca
  // lo agota, asi que corre igual de bien la primera vez o la enesima.
  standard: {
    name: 'PW Test - Paquete Estandar',
    destination: 'Ciudad de Prueba',
    description: 'Paquete de prueba automatizada (Playwright) para Epica 4/5.',
    startDate: '2026-12-01',
    endDate: '2026-12-10',
    price: 100.0,
    totalSlots: 500,
    includedServices: 'N/A',
    restrictions: 'N/A',
    travelType: 'Prueba',
    season: 'N/A',
  },

  // Criterio 2 (Epica 4): paquete barato usado para acumular 3 reservas
  // CONFIRMED previas (precondicion "cliente frecuente"), sin gastar de mas.
  cheapForFrequentHistory: {
    name: 'PW Test - Paquete Historial Frecuente',
    destination: 'Ciudad de Prueba',
    description: 'Paquete de prueba automatizada para construir historial de cliente frecuente.',
    startDate: '2026-12-01',
    endDate: '2026-12-05',
    price: 10.0,
    totalSlots: 500,
    includedServices: 'N/A',
    restrictions: 'N/A',
    travelType: 'Prueba',
    season: 'N/A',
  },

  // Criterio 3 (Epica 4): solo 2 cupos totales. Como el intento de reservar 5
  // SIEMPRE debe fallar, este paquete nunca pierde cupos entre corridas.
  lowSlots: {
    name: 'PW Test - Paquete Cupos Limitados',
    destination: 'Ciudad de Prueba',
    description: 'Paquete de prueba automatizada con cupos limitados (Epica 4, Criterio 3).',
    startDate: '2026-12-01',
    endDate: '2026-12-03',
    price: 50.0,
    totalSlots: 2,
    includedServices: 'N/A',
    restrictions: 'N/A',
    travelType: 'Prueba',
    season: 'N/A',
  },
};

const users = {
  // Cliente "nuevo" (sin historial), usado en Epica 4/Criterio 1 y Epica 5.
  client1: { fullName: 'Playwright Cliente Uno', email: 'pw.cliente1@example.com' },
  // Cliente dedicado al escenario de cliente frecuente (Epica 4/Criterio 2).
  frequentClient: { fullName: 'Playwright Cliente Frecuente', email: 'pw.frecuente@example.com' },
  // Cliente dedicado al escenario de cupos insuficientes (Epica 4/Criterio 3).
  client3: { fullName: 'Playwright Cliente Tres', email: 'pw.cliente3@example.com' },
  // Clientes dedicados a cada criterio de Epica 5 (cada uno con su propia
  // reserva PENDING, para que los 3 tests de pago no interfieran entre si).
  paymentClient1: { fullName: 'Playwright Pago Uno', email: 'pw.pago1@example.com' },
  paymentClient2: { fullName: 'Playwright Pago Dos', email: 'pw.pago2@example.com' },
  paymentClient3: { fullName: 'Playwright Pago Tres', email: 'pw.pago3@example.com' },
};

module.exports = { packages, users };
