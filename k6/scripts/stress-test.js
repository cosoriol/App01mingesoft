// Stress testing (Epica 7, Evaluacion 3): sube la carga progresivamente hasta
// encontrar el punto de quiebre (error rate > 50% o p95 > 5000ms). Empieza en
// 50 VUs y sube +10 cada minuto -- igual que especifica la estrategia.
//
// k6 no puede "abortar cuando se detecta el quiebre" a mitad de una corrida
// de forma nativa por VU; en cambio, cada escalon de 1 minuto se evalua
// aparte en el analisis posterior (results/stress.json trae metricas por
// tiempo). Se corre hasta un techo razonable (150 VUs / ~11 min) dado que la
// maquina de pruebas tiene recursos limitados (ver nota en el README de k6).
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const ADMIN_ID = __ENV.ADMIN_ID || '2';

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '1m', target: 60 },
    { duration: '1m', target: 70 },
    { duration: '1m', target: 80 },
    { duration: '1m', target: 90 },
    { duration: '1m', target: 100 },
    { duration: '1m', target: 110 },
    { duration: '1m', target: 120 },
    { duration: '1m', target: 130 },
    { duration: '1m', target: 140 },
    { duration: '1m', target: 150 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    // No se usa "abortOnFail" a proposito: queremos ver los datos de TODOS
    // los escalones (incluido el quiebre), no cortar la corrida al primer
    // umbral roto.
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.5'],
  },
};

export default function () {
  const url = `${BASE_URL}/api/reports/sales?startDate=2026-01-01&endDate=2026-12-31&userId=${ADMIN_ID}`;
  const response = http.get(url);

  check(response, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
