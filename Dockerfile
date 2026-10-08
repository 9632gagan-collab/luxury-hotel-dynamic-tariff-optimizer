# ==============================================================================
# ROYAL INDIAN STAYS - PRODUCTION CONTAINER DOCKERFILE
# Multi-stage build for optimal performance, minimal footprint and maximum security
# ==============================================================================

# ----------------- Stage 1: Build Application Artifact -----------------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Maven Wrapper and POM first to leverage Docker layer caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Grant execution rights to the Maven wrapper
RUN chmod +x ./mvnw

# Download dependencies offline
RUN ./mvnw dependency:go-offline -B

# Copy project source code
COPY src ./src

# Build production JAR package without running unit tests (done in CI/CD pipeline)
RUN ./mvnw clean package -DskipTests -B

# ----------------- Stage 2: Production Runtime -----------------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root system group and user for security hardening
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR from builder stage
COPY --from=builder /build/target/*.jar /app/app.jar

# Adjust ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose default HTTP port
EXPOSE 8080

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

# Container Healthcheck via /health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/health || exit 1

# Launch Application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
