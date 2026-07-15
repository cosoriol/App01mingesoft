# Despliegue de TravelAgency en Producción

## Arquitectura

```
                    Internet
                       |
                  puerto 80
                       v
              +-------------------+
              |   nginx-balancer   |   <- único punto de entrada público
              +-------------------+
               /api/  |      | resto (/)
                 v     |      v
   +-------------------+   +----------+
   | backend1/2/3:8080 |   | frontend |
   | (least_conn)       |   +----------+
   +-------------------+
             |
             v
          mysql:3306
```

- `nginx-balancer` (puerto 80) reparte `/api/**` entre las 3 réplicas del
  backend y reenvía todo lo demás al contenedor `frontend`, que sirve la SPA.
- El frontend también tiene su propio nginx con un `location /api/` que
  reenvía a `nginx-balancer` — sirve si alguien accede directo al puerto
  `3000` en vez del `80`. En ambos casos las llamadas del navegador son
  relativas (`/api/...`), nunca a una URL absoluta.
- MySQL no se expone al host: solo es alcanzable desde la red interna
  `travel-network`.

## Requisitos

- Docker y Docker Compose instalados
- Acceso a DigitalOcean o servidor en la nube
- Cuenta en Docker Hub con permiso de push sobre `cosoriol/*`

## Paso 1: Buildear y pushear imágenes (LOCAL)

```bash
docker login
./deploy.sh
```

O manualmente:

```bash
docker build -t cosoriol/travel-agency-backend:latest ./backend
docker push cosoriol/travel-agency-backend:latest
docker build -t cosoriol/travel-agency-frontend:latest ./frontend
docker push cosoriol/travel-agency-frontend:latest
```

## Paso 2: Crear servidor en DigitalOcean

- Ubuntu 22.04 LTS
- Tamaño: $12/mes mínimo (2 vCPU / 2GB RAM recomendado para correr 3 réplicas
  del backend + MySQL + nginx)
- Copiar la IP pública

## Paso 3: Desplegar en el servidor

```bash
ssh root@{IP_PUBLICA}
apt update && apt upgrade -y
curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh

mkdir ~/travelagency && cd ~/travelagency
# Desde tu máquina local:
scp docker-compose.yml nginx-balancer.conf root@{IP_PUBLICA}:~/travelagency/
scp .env root@{IP_PUBLICA}:~/travelagency/   # revisa antes las contraseñas

# En el servidor:
docker-compose up -d
docker-compose ps
```

`docker-compose ps` debe mostrar `mysql`, `backend1/2/3`, `frontend` y
`nginx-balancer` en estado `Up (healthy)` (las réplicas del backend tardan
~30-40s en pasar el healthcheck la primera vez, mientras arranca Spring Boot).

## Paso 4: Acceder

```
http://{IP_PUBLICA}
```

(puerto 80, sin necesidad de especificar puerto — es el que sirve
`nginx-balancer`).

## Antes de ir a un dominio real

- Cambiar `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` y `SPRING_DATASOURCE_PASSWORD`
  en `.env` por valores únicos y fuertes.
- Restringir `APP_CORS_ALLOWED_ORIGINS` en `.env` a tu dominio real
  (ej. `https://travelagency.com`) en vez de `*`.
- Poner TLS/HTTPS delante de `nginx-balancer` (Let's Encrypt/Certbot, o un
  balanceador de DigitalOcean).
- Quitar los puertos `8081:8080`, `8082:8080`, `8083:8080` del
  `docker-compose.yml` — hoy están abiertos solo para poder debuggear cada
  réplica del backend por separado; en producción real todo el tráfico
  debería entrar únicamente por `nginx-balancer` en el puerto 80.

## Comandos útiles

```bash
docker-compose ps                 # Ver estado
docker-compose logs -f            # Ver logs de todo
docker-compose logs -f backend1   # Ver logs de una réplica
docker-compose restart backend1   # Reiniciar un servicio
docker-compose down               # Detener todo (conserva el volumen de MySQL)
docker-compose down -v            # Detener todo y borrar los datos de MySQL
```

## Despliegue automatizado con Jenkins (opcional)

El `Jenkinsfile` en la raíz del repo automatiza los pasos 1 y 3 de arriba
(build + push de imágenes, y despliegue por SSH) cada vez que se pushea a
`main`.

Requiere configurar en Jenkins (Manage Jenkins → Credentials):

- `docker-hub-credentials` — Username with password (cuenta con permiso de
  push sobre `cosoriol/*`).
- `digitalocean-ssh` — SSH Username with private key (usuario `root`, clave
  privada del droplet; en local esa clave vive en `~/.ssh/digitalocean_rsa`).
- `do-server-ip` — Secret text con la IP pública del droplet.

Y en el droplet, `/root/travelagency/` debe existir con un `.env` ya
configurado (el pipeline nunca toca `.env`: solo sincroniza
`docker-compose.yml` y `nginx-balancer.conf`, hace `docker pull` de las
imágenes `:latest` y `docker-compose up -d`).

Para disparo automático por push, agregar un webhook en GitHub
(`Settings → Webhooks`) apuntando a `http://{jenkins-host}:8080/github-webhook/`.
