# --- STAGE 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn ./.mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# --- STAGE 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring && \
    mkdir -p /app/uploads && chown -R spring:spring /app/uploads

USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

# Répertoire pour les photos de profil
ENV APP_PHOTO_DIRECTORY=/app/uploads/

# Health check sur le port 8085
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

EXPOSE 8085

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]