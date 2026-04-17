# ╔════════════════════════════════════════════════════════════════════════════╗
# ║                    DOCKERFILE - MEDILABO FRONTEND                          ║
# ║                    Multi-stage build for Angular                           ║
# ╚════════════════════════════════════════════════════════════════════════════╝

<<<<<<< HEAD
=======
# Stage 1: Build
>>>>>>> origin/main
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

<<<<<<< HEAD
# ╔════════════════════════════════════════════════════════════════════════════╗
# ║                    STAGE 2: RUNTIME (NGINX)                                ║
# ╚════════════════════════════════════════════════════════════════════════════╝
=======
# Stage 2: Runtime avec Nginx
>>>>>>> origin/main
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Supprimer la config par défaut
RUN rm -rf ./*

<<<<<<< HEAD
# Copier le build Angular (Cette ligne est passée en CACHED, donc elle est correcte !)
COPY --from=builder /app/dist/medi-labo-solutions-ui/browser/ .

# CRÉER LE DOSSIER TEMPLATES ET LE FICHIER (Ajout de mkdir -p)
RUN mkdir -p /etc/nginx/templates && \
    echo 'server { \
=======
# Copier le build Angular
COPY --from=builder /app/dist/medi-labo-solutions-ui/browser .

# Config Nginx pour SPA Angular
RUN echo 'server { \
>>>>>>> origin/main
    listen 80; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
<<<<<<< HEAD
    location /api/ { \
        proxy_pass ${API_URL}; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
    } \
}' > /etc/nginx/templates/default.conf.template

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
=======
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
>>>>>>> origin/main
