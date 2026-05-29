# =============================================================================
# ZENENATION BACKEND — Dockerfile
# Multi-stage build: Maven build → slim Java runtime
# =============================================================================

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy built JAR
COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]