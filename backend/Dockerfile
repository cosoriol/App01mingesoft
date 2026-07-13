# ============================================================
# Etapa 1: Build (compila el JAR con Maven)
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copiar primero solo el pom.xml para aprovechar el cache de capas de Docker:
# si no cambian las dependencias, "mvn dependency" no se vuelve a descargar
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ============================================================
# Etapa 2: Runtime (imagen final, liviana)
# ============================================================
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Crear usuario no-root: el proceso Java no debe correr como root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el JAR compilado desde la etapa de build
COPY --from=builder --chown=appuser:appgroup /build/target/travelagency-backend-*.jar app.jar

EXPOSE 8080

# Health check: no hay server.servlet.context-path (cada controller ya define
# /api/... en su @RequestMapping), asi que actuator queda en /actuator/health
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
