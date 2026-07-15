#!/bin/bash
# Volume testing (Epica 7, Evaluacion 3): mide el reporte de ventas con
# 500/1000/5000/10000 reservas CONFIRMED en la base (acumulativo: cada nivel
# suma sobre el anterior). Los datos sinteticos usan un usuario/paquete
# dedicados ("K6 Volume Test") para poder identificarlos y borrarlos despues
# (ver cleanup-volume-test.sh).
set -e

source ~/App01Mingesoft/.env
PACKAGE_ID=16
USER_ID=12
ADMIN_ID=2
LOAD_VUS=30
LOAD_DURATION=30s

mysql_exec() {
  # "|| true": si mysql no imprime ninguna advertencia, "grep -v Warning" no
  # selecciona ninguna linea y sale con status 1 -- eso NO es un error real,
  # pero con "set -e" abortaria el script igual. El resultado real del INSERT
  # ya se verifica aparte con current_count().
  docker exec -i app01mingesoft_mysql_1 mysql -uroot -p"$MYSQL_ROOT_PASSWORD" travelagency_db 2>&1 | grep -v "Warning" || true
}

current_count() {
  docker exec app01mingesoft_mysql_1 mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e \
    "SELECT COUNT(*) FROM bookings WHERE user_id=$USER_ID AND package_id=$PACKAGE_ID;" \
    travelagency_db 2>/dev/null
}

cd ~/App01Mingesoft/k6
export PATH="$HOME/.local/bin:$PATH"

for target in 500 1000 5000 10000; do
  have=$(current_count)
  need=$((target - have))
  echo "== Nivel $target: hay $have, faltan $need =="
  if [ "$need" -gt 0 ]; then
    python3 scripts/generate_volume_data.py "$need" "$PACKAGE_ID" "$USER_ID" | mysql_exec
  fi

  total_now=$(current_count)
  echo "   Total sintetico ahora: $total_now"

  echo "   Corriendo k6 (VUS=$LOAD_VUS, $LOAD_DURATION)..."
  VUS=$LOAD_VUS DURATION=$LOAD_DURATION RAMP_TIME=5s ADMIN_ID=$ADMIN_ID \
    k6 run --summary-export="results/volume-${target}.json" scripts/load-test.js > "results/volume-${target}.log" 2>&1
  echo "   OK nivel $target"
done

echo "Volume test completo."
