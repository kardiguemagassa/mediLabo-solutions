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
    mkdir -p /app/src/main/resources/keys && \
    chown -R spring:spring /app/src/main/resources/keys

COPY --from=builder /app/target/*.jar app.jar

# Clés RSA (déposées par le pipeline Jenkins avant docker build)
COPY --chown=spring:spring src/main/resources/keys/private.key /app/src/main/resources/keys/private.key
COPY --chown=spring:spring src/main/resources/keys/public.key /app/src/main/resources/keys/public.key
RUN chmod 600 /app/src/main/resources/keys/private.key && \
    chmod 644 /app/src/main/resources/keys/public.key

USER spring:spring

ENV RSA_PRIVATE_KEY_PATH=/app/src/main/resources/keys/private.key
ENV RSA_PUBLIC_KEY_PATH=/app/src/main/resources/keys/public.key

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9001/actuator/health || exit 1

EXPOSE 9001

ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]