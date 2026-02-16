# --- STAGE 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 1. Optimisation du cache : on copie d'abord les fichiers de structure Maven
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
# On télécharge les dépendances (cette couche sera mise en cache)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 2. On copie le code source et on compile
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# --- STAGE 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sécurité : utilisateur non-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Récupération du JAR compilé
COPY --from=builder /app/target/*.jar app.jar

# Health check spécifique au port 8083
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

EXPOSE 8083

# Optimisation JVM (Mémoire et rapidité de démarrage)
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]