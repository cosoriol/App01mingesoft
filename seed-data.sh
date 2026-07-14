#!/bin/bash
# ============================================================
# Datos de prueba para App01Mingesoft (Epicas 1-7)
# ============================================================
# Reemplaza el enfoque de "init-data.sql" original: en vez de INSERTs
# crudos (que requerian un hash BCrypt real, el JSON exacto de
# discountDetails y podian chocar con IDs ya existentes), este script
# siembra los datos llamando a la API real via el balanceador
# (localhost:80). Asi cada fila pasa por las mismas reglas de negocio
# que un usuario real (hash de password, calculo de descuentos,
# timestamps), y es seguro correrlo mas de una vez (idempotente:
# usuarios/paquetes ya existentes se detectan y no se duplican).
set -e

# Nota: "Inside.2009" NO pasa la validacion real de fortaleza del registro (el "."
# no esta en el set de caracteres especiales permitidos @#$%^&+=!*) -- se usa una
# variante que si cumple la regla, para poder crear las cuentas via la API real.
BASE_URL="http://localhost/api"
PASSWORD="Inside2009!"
source "$(dirname "$0")/.env" 2>/dev/null

mysql_query() {
  docker exec app01mingesoft_mysql_1 mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "$1" travelagency_db 2>/dev/null
}

# Registra un usuario si no existe; devuelve su id (via consulta directa a MySQL,
# mas simple que parsear el 409/400 de "email ya registrado").
ensure_user() {
  local full_name="$1" email="$2"
  local existing_id
  existing_id=$(mysql_query "SELECT id FROM users WHERE email='$email';")
  if [ -n "$existing_id" ]; then
    echo "$existing_id"
    return
  fi
  curl -s -X POST "$BASE_URL/auth/register" -H "Content-Type: application/json" -d "{
    \"fullName\": \"$full_name\",
    \"email\": \"$email\",
    \"password\": \"$PASSWORD\"
  }" >/dev/null
  mysql_query "SELECT id FROM users WHERE email='$email';"
}

echo "== Usuarios =="
ADMIN_ID=$(ensure_user "Admin TravelAgency" "admin@travelagency.cl")
mysql_query "UPDATE users SET role='ADMIN' WHERE id=$ADMIN_ID;"
JUAN_ID=$(ensure_user "Juan Pérez" "juan@example.com")
MARIA_ID=$(ensure_user "María García" "maria@example.com")
CARLOS_ID=$(ensure_user "Carlos López" "carlos@example.com")
echo "  admin=$ADMIN_ID (ADMIN)  juan=$JUAN_ID  maria=$MARIA_ID  carlos=$CARLOS_ID"

# Crea un paquete si no existe uno con ese nombre; devuelve su id.
ensure_package() {
  local name="$1" destination="$2" description="$3" start="$4" end="$5" price="$6" \
        slots="$7" services="$8" restrictions="$9" type="${10}" season="${11}"
  local existing_id
  existing_id=$(mysql_query "SELECT id FROM travel_packages WHERE name='$name' LIMIT 1;")
  if [ -n "$existing_id" ]; then
    echo "$existing_id"
    return
  fi
  curl -s -X POST "$BASE_URL/packages?userId=$ADMIN_ID" -H "Content-Type: application/json" -d "{
    \"name\": \"$name\", \"destination\": \"$destination\", \"description\": \"$description\",
    \"startDate\": \"$start\", \"endDate\": \"$end\", \"price\": $price, \"totalSlots\": $slots,
    \"includedServices\": \"$services\", \"restrictions\": \"$restrictions\",
    \"travelType\": \"$type\", \"season\": \"$season\"
  }" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])"
}

echo "== Paquetes =="
P1=$(ensure_package "Tour Machu Picchu 5 días" "Perú" "Incluye vuelos y hotel 5 estrellas" \
  "2026-08-15" "2026-08-20" 1500.00 20 "Vuelos,Hotel,Tours,Seguro" "Mayor de 18 años" "Aventura" "Verano")
P2=$(ensure_package "Playa Cancún 7 días" "México" "Resort todo incluido en Cancún" \
  "2026-09-01" "2026-09-08" 1200.00 30 "Resort,Playa,Actividades acuáticas" "Ninguna" "Playa" "Verano")
P3=$(ensure_package "Europa Clásica 10 días" "Europa" "París, Ámsterdam, Berlín, Praga" \
  "2026-07-15" "2026-07-25" 2500.00 15 "Vuelos,Hotels,Tours,Guía" "Pasaporte requerido" "Cultural" "Verano")
P4=$(ensure_package "Cartagena Romántica 4 días" "Colombia" "Conoce la ciudad amurallada" \
  "2026-09-15" "2026-09-19" 800.00 25 "Hotel,Tours,Cena romántica" "Ninguna" "Romántico" "Otoño")
P5=$(ensure_package "Atacama Aventura 3 días" "Chile" "Desierto de Atacama con astrónomos" \
  "2026-08-10" "2026-08-13" 600.00 20 "Hotel,Tours,Comidas" "Altura - consultar médico" "Aventura" "Verano")
P6=$(ensure_package "Crucero Caribe 7 días" "Caribe" "Crucero con paradas en 4 islas" \
  "2026-10-05" "2026-10-12" 1800.00 40 "Crucero,Cabina,Comidas,Actividades" "Ninguna" "Playa" "Otoño")
echo "  paquetes: $P1 $P2 $P3 $P4 $P5 $P6"

# Crea una reserva (POST /api/bookings) y devuelve su id + totalAmount ("id totalAmount").
create_booking() {
  local user_id="$1" package_id="$2" passengers="$3"
  curl -s -X POST "$BASE_URL/bookings?userId=$user_id" -H "Content-Type: application/json" -d "{
    \"packageId\": $package_id, \"passengerCount\": $passengers
  }" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d['id'], d['totalAmount'])"
}

pay_booking() {
  local user_id="$1" booking_id="$2" amount="$3"
  curl -s -X POST "$BASE_URL/payments?userId=$user_id" -H "Content-Type: application/json" -d "{
    \"bookingId\": $booking_id, \"amount\": $amount, \"paymentMethod\": \"CREDIT_CARD\",
    \"cardNumber\": \"4111111111111111\", \"expirationDate\": \"12/29\", \"cvv\": \"123\",
    \"cardHolderName\": \"Test User\"
  }" >/dev/null
}

echo "== Reservas y pagos =="
# Si ya existen reservas de estos 3 clientes (corrida anterior), no duplicar.
EXISTING=$(mysql_query "SELECT COUNT(*) FROM bookings WHERE user_id IN ($JUAN_ID,$MARIA_ID,$CARLOS_ID);")
if [ "$EXISTING" -gt 0 ]; then
  echo "  ya hay $EXISTING reservas de juan/maria/carlos, no se vuelven a crear"
else
  read -r B1 B1_AMT <<< "$(create_booking "$JUAN_ID" "$P1" 2)";   pay_booking "$JUAN_ID" "$B1" "$B1_AMT"
  read -r B2 B2_AMT <<< "$(create_booking "$JUAN_ID" "$P3" 1)";   pay_booking "$JUAN_ID" "$B2" "$B2_AMT"
  read -r B3 B3_AMT <<< "$(create_booking "$MARIA_ID" "$P2" 4)";  pay_booking "$MARIA_ID" "$B3" "$B3_AMT"
  read -r B4 B4_AMT <<< "$(create_booking "$MARIA_ID" "$P5" 3)"   # queda PENDING (sin pagar)
  read -r B5 B5_AMT <<< "$(create_booking "$CARLOS_ID" "$P4" 2)"; pay_booking "$CARLOS_ID" "$B5" "$B5_AMT"
  read -r B6 B6_AMT <<< "$(create_booking "$CARLOS_ID" "$P6" 5)"; pay_booking "$CARLOS_ID" "$B6" "$B6_AMT"

  # Reserva cancelada: se crea y se cancela de inmediato
  read -r B7 B7_AMT <<< "$(create_booking "$JUAN_ID" "$P4" 1)"
  curl -s -X PATCH "$BASE_URL/bookings/$B7/cancel?userId=$JUAN_ID" >/dev/null

  # Reserva expirada: se crea y se fuerza el estado (en vez de esperar los 30 min reales
  # que tarda expireStaleBookings()); se liberan los cupos a mano, igual que haria esa tarea.
  read -r B8 B8_AMT <<< "$(create_booking "$MARIA_ID" "$P1" 3)"
  mysql_query "UPDATE bookings SET status='EXPIRED', created_at = created_at - INTERVAL 1 HOUR WHERE id=$B8;"
  mysql_query "UPDATE travel_packages SET booked_slots = booked_slots - 3 WHERE id=$P1;"

  echo "  reservas creadas: $B1 $B2 $B3 $B4(pendiente) $B5 $B6 $B7(cancelada) $B8(expirada)"
fi

echo ""
echo "== Resumen =="
mysql_query "SELECT status, COUNT(*) FROM bookings GROUP BY status;"
echo ""
echo "Credenciales de prueba (todas con password: $PASSWORD):"
echo "  ADMIN:  admin@travelagency.cl"
echo "  CLIENT: juan@example.com / maria@example.com / carlos@example.com"
