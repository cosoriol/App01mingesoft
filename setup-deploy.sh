#!/bin/bash
# ============================================================
# SETUP DEPLOY - Proyecto TravelAgency
# ============================================================
# Crea los archivos de Docker (Dockerfile backend/frontend,
# docker-compose.yml) y el README del proyecto.
#
# Puede ejecutarse solo, o ser llamado por setup-project.sh
#
# USO: Ejecutar desde ~/App01Mingesoft/
#   chmod +x setup-deploy.sh
#   ./setup-deploy.sh
# ============================================================

# ============================================================
# 3. ARCHIVOS DE DOCKER (para despliegue)
# ============================================================
echo ""
echo "🐳 Creando archivos Docker..."

# ---- Dockerfile Backend ----
cat > backend/Dockerfile << 'DBEOF'
# Imagen base: Java 17
FROM eclipse-temurin:17-jdk-alpine

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el JAR compilado
COPY target/*.jar app.jar

# Puerto que expone la aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
DBEOF

# ---- Dockerfile Frontend ----
cat > frontend/Dockerfile << 'DFEOF'
# Etapa 1: Compilar la app React
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Etapa 2: Servir con Nginx
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
DFEOF

# ---- nginx.conf para frontend ----
cat > frontend/nginx.conf << 'NGEOF'
server {
    listen 80;
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
NGEOF

# ---- docker-compose.yml ----
cat > docker-compose.yml << 'DCEOF'
version: '3.8'

services:
  # Base de datos MySQL
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: travelagency_db
    ports:
      - "3306:3306"
    volumes:
      - db_data:/var/lib/mysql

  # Backend (3 réplicas)
  backend:
    image: TU_USUARIO_DOCKERHUB/travelagency-backend:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/travelagency_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root123
    depends_on:
      - db
    deploy:
      replicas: 3

  # Frontend
  frontend:
    image: TU_USUARIO_DOCKERHUB/travelagency-frontend:latest
    ports:
      - "80:80"

  # Nginx como balanceador de carga
  nginx:
    image: nginx:alpine
    ports:
      - "8080:80"
    volumes:
      - ./nginx-lb.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend

volumes:
  db_data:
DCEOF

echo "   ✅ Docker files creados"

# ============================================================
# 4. README del proyecto
# ============================================================
cat > README.md << 'READMEEOF'
# TravelAgency - Plataforma Web de Paquetes Turísticos

## Descripción
Aplicación web monolítica para la gestión y venta de paquetes turísticos.

## Tecnologías
- **Frontend:** ReactJS
- **Backend:** Spring Boot (Java 17)
- **Base de Datos:** MySQL 8.0
- **Autenticación:** Keycloak
- **Contenedores:** Docker + Docker Compose
- **CI/CD:** Jenkins
- **Testing:** JUnit 5 + JaCoCo

## Estructura del Proyecto
```
App01Mingesoft/
├── backend/                  # API REST (Spring Boot)
│   ├── src/main/java/com/travelagency/
│   │   ├── config/           # Configuraciones (seguridad, CORS)
│   │   ├── controller/       # Capa Controller (endpoints REST)
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── entity/           # Capa Entity (tablas de la BD)
│   │   ├── exception/        # Excepciones personalizadas
│   │   ├── repository/       # Capa Repository (acceso a datos)
│   │   └── service/          # Capa Service (lógica de negocio)
│   └── src/test/             # Pruebas unitarias
├── frontend/                 # Interfaz web (React)
│   ├── src/
│   │   ├── components/       # Componentes reutilizables
│   │   ├── pages/            # Páginas de la aplicación
│   │   └── services/         # Conexión con el backend
├── docker-compose.yml        # Orquestación de contenedores
└── README.md
```

## Cómo ejecutar localmente

### Backend
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm start
```

## Épicas implementadas
1. Gestión de usuarios y clientes
2. Publicación de paquetes turísticos
3. Exploración y búsqueda de paquetes
4. Proceso de reserva en línea
5. Gestión de pagos en línea
6. Confirmación y seguimiento de reservas
7. Generación de reportes
READMEEOF

echo "   ✅ README.md creado"

