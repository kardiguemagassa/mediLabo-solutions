# ╔════════════════════════════════════════════════════════════════════════════╗
# ║                    DOCKERFILE - DISCOVERY SERVER                           ║
# ║                    Multi-stage build for Spring Boot                       ║
# ╚════════════════════════════════════════════════════════════════════════════╝

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY .mvn ./.mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sécurité: utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copier le JAR
COPY --from=builder /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

# Port
EXPOSE 8761

# Démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
