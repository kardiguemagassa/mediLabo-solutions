# ╔════════════════════════════════════════════════════════════════════════════╗
# ║                    DOCKERFILE - DISCOVERY SERVER                           ║
# ║                    Multi-stage build for Spring Boot                       ║
# ╚════════════════════════════════════════════════════════════════════════════╝

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# --- OPTIMISATION CACHE MAVEN ---
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
# Télécharge les dépendances sans compiler le code (couche réutilisable)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# --- COMPILATION ---
COPY src ./src
RUN ./mvnw clean package -DskipTests -q && \
    rm -rf ~/.m2/repository

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sécurité: utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

EXPOSE 8761

# --- OPTIMISATION JVM POUR DOCKER ---
# UseContainerSupport permet à Java de lire les limites CPU/RAM de Docker
# MaxRAMPercentage évite les crashs OutOfMemory (OOM)
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]
