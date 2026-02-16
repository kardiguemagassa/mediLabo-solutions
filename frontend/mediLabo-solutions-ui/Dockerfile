# ╔════════════════════════════════════════════════════════════════════════════╗
# ║                    DOCKERFILE - MEDILABO FRONTEND                          ║
# ║                    Multi-stage build for Angular                           ║
# ╚════════════════════════════════════════════════════════════════════════════╝

FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

# ╔════════════════════════════════════════════════════════════════════════════╗
# ║                    STAGE 2: RUNTIME (NGINX)                                ║
# ╚════════════════════════════════════════════════════════════════════════════╝
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Supprimer la config par défaut
RUN rm -rf ./*

# Copier le build Angular (Cette ligne est passée en CACHED, donc elle est correcte !)
COPY --from=builder /app/dist/medi-labo-solutions-ui/browser/ .

# CRÉER LE DOSSIER TEMPLATES ET LE FICHIER (Ajout de mkdir -p)
RUN mkdir -p /etc/nginx/templates && \
    echo 'server { \
    listen 80; \
    server_name localhost; \
    root /usr/share/nginx/html; \
    index index.html; \
    location / { \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/ { \
        proxy_pass ${API_URL}; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
    } \
}' > /etc/nginx/templates/default.conf.template

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]