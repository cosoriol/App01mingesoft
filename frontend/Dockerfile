# ============================================================
# Etapa 1: Build (compila la app de Create React App)
# ============================================================
FROM node:18-alpine AS builder
WORKDIR /app

# Copiar primero solo los manifiestos para aprovechar el cache de capas
COPY package*.json ./
RUN npm install

COPY . .
# react-scripts build genera la salida en ./build (no ./dist)
RUN npm run build

# ============================================================
# Etapa 2: Runtime (Nginx sirviendo los estaticos)
# ============================================================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Limpiar el contenido default de nginx antes de copiar el build
RUN rm -rf ./*

COPY --from=builder /app/build .
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
