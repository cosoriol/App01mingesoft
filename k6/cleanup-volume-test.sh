#!/bin/bash
# Borra los datos sinteticos de la prueba de volumen (Epica 7, Evaluacion 3):
# el usuario y paquete "K6 Volume Test" dedicados, y todas sus reservas/pagos.
# No toca ningun dato real de la app.
set -e
source ~/App01Mingesoft/.env

PACKAGE_ID=16
USER_ID=12

docker exec -i app01mingesoft_mysql_1 mysql -uroot -p"$MYSQL_ROOT_PASSWORD" travelagency_db 2>&1 <<SQL | grep -v "Warning" || true
DELETE FROM payments WHERE booking_id IN (SELECT id FROM bookings WHERE user_id=$USER_ID AND package_id=$PACKAGE_ID);
DELETE FROM bookings WHERE user_id=$USER_ID AND package_id=$PACKAGE_ID;
DELETE FROM travel_packages WHERE id=$PACKAGE_ID;
DELETE FROM users WHERE id=$USER_ID;
SQL

echo "Datos sinteticos de volume-test eliminados."
docker exec app01mingesoft_mysql_1 mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e \
  "SELECT COUNT(*) FROM bookings WHERE user_id=$USER_ID;" travelagency_db 2>/dev/null
