# ╔════════════════════════════════════════════════════════════════════════════╗
# ║                    DOCKERFILE - GATEWAY SERVER                             ║
# ║                    Multi-stage build for Spring Boot                       ║
# ╚════════════════════════════════════════════════════════════════════════════╝

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# --- OPTIMISATION CACHE MAVEN ---
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
# Télécharge les dépendances (cette étape est mise en cache tant que le pom.xml ne change pas)
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

# Health check (Port 8080 pour le Gateway)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# --- OPTIMISATION JVM POUR DOCKER ---
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]