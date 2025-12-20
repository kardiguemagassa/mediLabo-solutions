# ╔════════════════════════════════════════════════════════════════════════════╗
# ║                    DOCKERFILE - MEDILABO FRONTEND                          ║
# ║                    Multi-stage build for Angular                           ║
# ╚════════════════════════════════════════════════════════════════════════════╝

# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

# Stage 2: Runtime avec Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Supprimer la config par défaut
RUN rm -rf ./*

# Copier le build Angular
COPY --from=builder /app/dist/medi-labo-solutions-ui/browser .

# Config Nginx pour SPA Angular
RUN echo 'server { \
    listen 80; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api { \
        proxy_pass http://gateway:8080; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
    } \
}' > /etc/nginx/conf.d/default.conf

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80 || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
