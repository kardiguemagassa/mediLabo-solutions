# --- STAGE 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 1. Optimisation du cache Maven : on télécharge d'abord les dépendances
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 2. Compilation du code source
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# --- STAGE 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 3. Sécurité : Création de l'utilisateur ET du répertoire pour les clés RSA
RUN addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/keys && chown -R spring:spring /app/keys

# 4. Passage en utilisateur non-root
USER spring:spring

# 5. Récupération du JAR
COPY --from=builder /app/target/*.jar app.jar

# 6. Variables d'environnement par défaut pour les clés (Surchargées par le docker-compose)
ENV RSA_PRIVATE_KEY_PATH=/app/keys/private.key
ENV RSA_PUBLIC_KEY_PATH=/app/keys/public.key

# 7. Health check (Port 9001 pour l'Auth Server)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9001/actuator/health || exit 1

EXPOSE 9001

# 8. Entrypoint optimisé pour Docker (Gestion mémoire et entropie)
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]