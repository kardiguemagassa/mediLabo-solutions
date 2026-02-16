# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# --- OPTIMISATION CACHE MAVEN (Comme ton Gateway) ---
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
# On télécharge les dépendances ici. Docker mettra cette couche en cache.
# Elle ne sera re-exécutée QUE si ton pom.xml change.
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# --- COMPILATION ---
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sécurité et Dossiers
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads

# Copie du JAR
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

EXPOSE 8082

# --- OPTIMISATION JVM (Manquante dans ton message précédent) ---
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]