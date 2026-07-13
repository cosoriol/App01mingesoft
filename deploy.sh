#!/bin/bash
set -e

# ============================================================
# Script de build + push de imagenes a Docker Hub
# ============================================================
# NOTA: no compila el JAR localmente antes de "docker build": el
# backend/Dockerfile ya es multi-stage y compila con Maven dentro del propio
# build de la imagen (COPY src + mvn package), asi que compilar dos veces
# seria redundante. La validacion de tests/compilacion la corre CI
# (.github/workflows/ci.yml) en cada push.

echo "Iniciando despliegue de TravelAgency..."

echo "Construyendo imagenes Docker..."
docker build -t cosoriol/travel-agency-backend:latest ./backend
docker build -t cosoriol/travel-agency-frontend:latest ./frontend

echo "Pusheando imagenes a Docker Hub..."
echo "(requiere 'docker login' previo con una cuenta con permiso sobre cosoriol/*)"
docker push cosoriol/travel-agency-backend:latest
docker push cosoriol/travel-agency-frontend:latest

echo "Despliegue completado. Imagenes listas en Docker Hub."
echo "Proximos pasos en el servidor:"
echo "  docker-compose up -d"
echo "  docker-compose ps"
