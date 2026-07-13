#!/bin/bash
# Sigue los logs de todos los servicios de App01Mingesoft.
cd "$(dirname "$0")"
docker-compose logs -f
