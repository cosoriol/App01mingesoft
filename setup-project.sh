#!/bin/bash
# ============================================================
# SCRIPT DE CONFIGURACIÓN - Proyecto TravelAgency
# ============================================================
# Este script NO crea archivos directamente: solo ORQUESTA,
# en orden, a tres scripts más pequeños y fáciles de leer:
#
#   1. setup-backend.sh   -> Backend (Spring Boot + Java)
#   2. setup-frontend.sh  -> Frontend (ReactJS)
#   3. setup-deploy.sh    -> Docker, docker-compose y README
#
# Si algo falla (ej: un typo en el backend), puedes corregir
# ese archivo puntual y volver a ejecutar SOLO ese script,
# sin tener que repetir todo el proceso.
#
# USO: Ejecutar desde ~/App01Mingesoft/
#   chmod +x setup-project.sh setup-backend.sh setup-frontend.sh setup-deploy.sh
#   ./setup-project.sh
# ============================================================

echo "🚀 Iniciando configuración del proyecto TravelAgency..."
echo ""

# Carpeta donde está este script, para poder llamar a los demás
# sin importar desde dónde se ejecute setup-project.sh
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Si un paso falla, detener todo (evita seguir con archivos a medio crear)
set -e

bash "$SCRIPT_DIR/setup-backend.sh"
bash "$SCRIPT_DIR/setup-frontend.sh"
bash "$SCRIPT_DIR/setup-deploy.sh"

# ============================================================
# RESUMEN FINAL
# ============================================================
echo ""
echo "============================================================"
echo "✅ ¡PROYECTO CONFIGURADO EXITOSAMENTE!"
echo "============================================================"
echo ""
echo "Estructura creada:"
echo ""
echo "  App01Mingesoft/"
echo "  ├── backend/              ← API REST (Spring Boot)"
echo "  │   ├── src/main/java/    ← Código fuente"
echo "  │   │   ├── entity/       ← 5 entidades (User, TravelPackage, Booking, Payment)"
echo "  │   │   ├── repository/   ← 4 repositorios"
echo "  │   │   ├── service/      ← 3 servicios (lógica de negocio)"
echo "  │   │   ├── controller/   ← 3 controladores (API REST)"
echo "  │   │   ├── dto/          ← DTOs de request"
echo "  │   │   ├── exception/    ← Manejo de errores"
echo "  │   │   └── config/       ← Configuración de seguridad"
echo "  │   └── pom.xml           ← Dependencias Maven"
echo "  ├── frontend/             ← Interfaz web (React)"
echo "  │   ├── src/pages/        ← Páginas (PackageList, PackageDetail)"
echo "  │   ├── src/services/     ← Conexión API"
echo "  │   └── package.json      ← Dependencias npm"
echo "  ├── docker-compose.yml    ← Orquestación Docker"
echo "  └── README.md"
echo ""
echo "Próximos pasos:"
echo "  1. cd backend && ./mvnw spring-boot:run"
echo "  2. cd frontend && npm install && npm start"
echo ""
