#!/bin/bash
# Levanta App01Mingesoft (monolito: mysql + 3 replicas backend + frontend + nginx-balancer).
set -e

echo "Iniciando App01Mingesoft (monolitico)..."
echo "Puertos: Frontend:3000, Nginx-balancer:80, Backend (debug):8001-8003"
cd "$(dirname "$0")"
docker-compose up -d
sleep 5
echo "App01Mingesoft iniciada"
docker-compose ps
