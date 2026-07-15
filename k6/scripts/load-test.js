// Load testing (Epica 7, Evaluacion 3): simula 10/50/100/200 usuarios
// concurrentes contra el endpoint de reportes (GET /api/reports/sales), el
// mas pesado del sistema (agrega reservas + pagos en un rango de fechas).
//
// Se corre UNA vez por nivel de carga (no en un solo ramp continuo), para
// poder comparar metricas limpias por nivel en la tabla del Documento 5:
//   VUS=10  DURATION=1m k6 run load-test.js
//   VUS=50  DURATION=1m k6 run load-test.js
//   VUS=100 DURATION=1m k6 run load-test.js
//   VUS=200 DURATION=1m k6 run load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const ADMIN_ID = __ENV.ADMIN_ID || '2';
const VUS = parseInt(__ENV.VUS || '10', 10);
const DURATION = __ENV.DURATION || '1m';
const RAMP_TIME = __ENV.RAMP_TIME || '10s';

export const options = {
  stages: [
    { duration: RAMP_TIME, target: VUS },
    { duration: DURATION, target: VUS },
    { duration: RAMP_TIME, target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.1'],
  },
};

export default function () {
  const url = `${BASE_URL}/api/reports/sales?startDate=2026-01-01&endDate=2026-12-31&userId=${ADMIN_ID}`;
  const response = http.get(url);

  check(response, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);
}
