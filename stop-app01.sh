#!/bin/bash
# Detiene App01Mingesoft (conserva el volumen de datos de MySQL).
set -e

echo "Deteniendo App01Mingesoft..."
cd "$(dirname "$0")"
docker-compose down
echo "App01Mingesoft detenida"
