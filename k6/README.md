# Pruebas de rendimiento (Evaluación 3, Fase 5 — Épica 7)

Pruebas de carga/estrés/volumen con [k6](https://k6.io) contra
`GET /api/reports/sales`, el endpoint agregado más pesado del sistema.

## Requisitos

- App01Mingesoft corriendo (`bash ~/App01Mingesoft/start-app01.sh`).
- `k6` en el PATH. Si no está instalado vía `apt`, se puede usar el binario
  standalone (no requiere sudo):
  ```bash
  curl -sL -o /tmp/k6.tar.gz "https://github.com/grafana/k6/releases/download/v2.1.0/k6-v2.1.0-linux-amd64.tar.gz"
  tar xzf /tmp/k6.tar.gz -C /tmp
  mv /tmp/k6-v2.1.0-linux-amd64/k6 ~/.local/bin/k6
  ```
- Recomendado: si App02Mingesoft también está corriendo en la misma máquina,
  considera pausarla (`docker-compose stop` en `App02Mingesoft/`) mientras
  corres las pruebas de carga — con poca RAM libre, competir por recursos
  distorsiona las mediciones (no es que la app falle, es que la máquina de
  pruebas se queda sin memoria). Reactivar después con `docker-compose start`.

## Load testing (10 / 50 / 100 / 200 usuarios)

```bash
for vus in 10 50 100 200; do
  VUS=$vus DURATION=45s RAMP_TIME=10s \
    k6 run --summary-export=results/load-${vus}.json scripts/load-test.js
done
```

## Stress testing (50 → 150 VUs, +10/min)

```bash
k6 run --summary-export=results/stress.json scripts/stress-test.js
```

## Volume testing (500 / 1000 / 5000 / 10000 registros, acumulativo)

Siembra datos sintéticos directo por SQL (mucho más rápido que por API para
miles de filas) en un usuario/paquete dedicados ("K6 Volume Test"), corre una
carga fija (30 VUs, 30s) por cada nivel, y guarda resultados. **Requiere**
que exista un usuario ADMIN (`admin@travelagency.cl`) y ajustar
`ADMIN_ID`/`PACKAGE_ID`/`USER_ID` en el script si tu entorno tiene IDs
distintos.

```bash
./run-volume-test.sh
```

Al terminar, limpiar los datos sintéticos (no debe quedar mezclado con datos
reales de demo):

```bash
./cleanup-volume-test.sh
```

## Generar el Excel comparativo

```bash
pip install --user openpyxl   # una sola vez
python3 build_report.py
```

Genera `Pruebas_Rendimiento_K6.xlsx` con 3 hojas (Load/Stress/Volume),
gráficos de P95 y tablas comparativas — a partir de los JSON reales en
`results/`, nunca con números inventados.

## Resultados de la última corrida (referencia)

| Métrica | 10 users | 50 users | 100 users | 200 users |
|---|---|---|---|---|
| P95 (ms) | 9.69 | 7.47 | 7.03 | 6.50 |
| Error rate | 0% | 0% | 0% | 0.01% (4/22102, no failures reales) |

**Stress**: sin punto de quiebre hasta 150 VUs (p95=5.99ms, 0% error). El
endpoint es liviano bajo el volumen de datos actual.

**Volume**: degradación ~lineal con la cantidad de filas — el cuello de
botella real es de **volumen de datos**, no de concurrencia:

| Registros | P95 (ms) |
|---|---|
| 500 | 8.26 |
| 1,000 | 17.49 |
| 5,000 | 59.76 |
| 10,000 | 110.42 |

Causa: `ReportService.getSalesReport()` trae TODAS las reservas del rango a
memoria (`bookingRepository.findBookingsByDateRange(...)`) y filtra/suma con
streams de Java, en vez de agregar en SQL. Con datasets grandes, esto
escalaría mal — la mejora sugerida es mover el filtro por estado y el `SUM`
a la consulta (JPQL/`@Query` con `WHERE status = 'CONFIRMED'` y
`SUM(totalAmount)`), en vez de traer todas las filas al backend.
