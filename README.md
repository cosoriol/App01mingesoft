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
