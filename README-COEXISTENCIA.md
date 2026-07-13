# Ejecutar App01Mingesoft junto a otro proyecto local (ej. App02)

App01Mingesoft es un monolito (Spring Boot + MySQL) dockerizado con 3 réplicas
de backend detrás de un balanceador nginx. Estos son los puertos que usa en
el **host** (fuera de los contenedores):

| Servicio                     | Puerto host | Notas |
|-------------------------------|-------------|-------|
| Frontend                      | `3000`      | React servido por nginx |
| Nginx-balancer (entrada única) | `80`        | Reparte `/api/**` entre las 3 réplicas del backend |
| Backend réplica 1 (debug)      | `8001`      | Solo para depurar una réplica puntual |
| Backend réplica 2 (debug)      | `8002`      | Idem |
| Backend réplica 3 (debug)      | `8003`      | Idem |
| MySQL                          | *(ninguno)* | No se expone al host, solo accesible dentro de `travel-network` |

MySQL no reserva ningún puerto en el host, así que no puede chocar con la
base de datos de otro proyecto sin importar qué puerto use esta (5432, 5433,
etc.) — la conexión entre contenedores usa siempre el puerto interno `3306`.

## Comandos

```bash
bash start-app01.sh    # levanta todo (docker-compose up -d)
bash logs-app01.sh     # sigue los logs de todos los servicios
bash stop-app01.sh     # detiene todo (conserva los datos de MySQL)
```

## Si otro proyecto (ej. App02) corre en la misma máquina

Solo hay conflicto real si el otro proyecto también reclama alguno de los
puertos de la tabla de arriba (`3000`, `80`, `8001-8003`). Si tu otro proyecto
usa, por ejemplo, `3001` para su frontend y `8080` para un API Gateway, no
hay ningún choque y ambos pueden correr al mismo tiempo con:

```bash
bash ~/App01Mingesoft/start-app01.sh
bash ~/OtroProyecto/start-app02.sh   # o el equivalente del otro proyecto
docker ps                             # confirmar que todos los contenedores de ambos estan Up
```

Si el otro proyecto SÍ pisa alguno de estos puertos, cambia el puerto host
(el número de la izquierda en `"host:contenedor"`) en `docker-compose.yml` de
uno de los dos proyectos — nunca hace falta tocar el puerto interno ni el
código de la aplicación.
