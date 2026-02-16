# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# --- OPTIMISATION CACHE MAVEN ---
# On copie d'abord les fichiers de configuration Maven
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
# On télécharge les dépendances. Cette couche sera réutilisée 
# tant que le pom.xml ne change pas (Gain de temps énorme)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# --- COMPILATION ---
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sécurité: utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copie du JAR avec attribution des droits à l'utilisateur spring
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Health check sur le port 8084 (Actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

EXPOSE 8084

# --- OPTIMISATION JVM POUR DOCKER ---
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]