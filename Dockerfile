# ==========================================
# Multi-stage Dockerfile for Spring Boot 3.2
# ==========================================

# 1. Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven descriptor and source code
COPY pom.xml .
COPY src ./src

# Build production executable JAR (skipping unit tests during packaging)
RUN mvn clean package -DskipTests

# 2. Production Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-privileged user for container security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Cloud providers inject $PORT dynamically
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
